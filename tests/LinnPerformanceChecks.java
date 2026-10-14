package dev.nino.squareglass;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Local protocol fixtures: no physical renderer, accounts or audio are used. */
public final class LinnPerformanceChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static byte[] bytes(String value){return value.getBytes(StandardCharsets.UTF_8);}
    static InetAddress lan()throws Exception {
        for(NetworkInterface network:Collections.list(NetworkInterface.getNetworkInterfaces()))
            for(InetAddress address:Collections.list(network.getInetAddresses()))
                if(address instanceof Inet4Address && address.isSiteLocalAddress())return address;
        throw new AssertionError("Private interface required for the local test server");
    }
    static void send(HttpExchange exchange,String body)throws IOException {
        byte[] payload=bytes(body);exchange.sendResponseHeaders(200,payload.length);exchange.getResponseBody().write(payload);exchange.close();
    }
    static final class Fixture implements AutoCloseable {
        final HttpServer server;
        final ExecutorService worker=Executors.newCachedThreadPool();
        final String base;
        final List<Long> queue=new ArrayList<>();
        final Map<Long,LinnQueueTransaction.Track> tracks=new HashMap<>();
        final Map<String,AtomicInteger> calls=new ConcurrentHashMap<>();
        boolean loseInsert,loseDelete;
        long next=100;
        long selected=42,pendingTrack;
        int trackPolls,seconds;
        boolean playing,premature;
        volatile boolean initialEventBeforeReply;
        Fixture(InetAddress address)throws Exception {
            server=HttpServer.create(new InetSocketAddress(address,0),0);server.setExecutor(worker);
            base="http://"+address.getHostAddress()+":"+server.getAddress().getPort();
            server.createContext("/device",this::description);
            server.createContext("/scpd",this::scpd);
            server.createContext("/control",this::control);
            server.createContext("/events",exchange -> {
                String kind=exchange.getRequestURI().getPath().substring("/events/".length());
                String callback=exchange.getRequestHeaders().getFirst("CALLBACK");
                if(initialEventBeforeReply && kind.equals("Playlist") && callback!=null){
                    worker.execute(() -> {try{LinnPerformanceChecks.notify(callback.substring(1,callback.length()-1),"uuid:fixture-Playlist",0,eventBody(),address);}catch(Exception failure){throw new RuntimeException(failure);}});
                    try{Thread.sleep(50);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
                }
                exchange.getResponseHeaders().add("SID","uuid:fixture-"+kind);
                exchange.getResponseHeaders().add("TIMEOUT","Second-300");send(exchange,"<ok/>");
            });
            server.start();
        }
        int count(String key){return calls.getOrDefault(key,new AtomicInteger()).get();}
        void hit(String key){calls.computeIfAbsent(key,ignored -> new AtomicInteger()).incrementAndGet();}
        void description(HttpExchange exchange)throws IOException {
            hit("description");String services="";
            for(String kind:Arrays.asList("Playlist","Product","Time","Volume","OAuth"))
                services+="<service><serviceType>urn:av-openhome-org:service:"+kind+":1</serviceType><controlURL>/control/"+kind+"</controlURL><eventSubURL>/events/"+kind+"</eventSubURL><SCPDURL>/scpd/"+kind+"</SCPDURL></service>";
            send(exchange,"<root><device><UDN>uuid:performance-test</UDN><friendlyName>Fixture</friendlyName><manufacturer>Linn</manufacturer><modelName>Majik</modelName><serviceList>"+services+"</serviceList></device></root>");
        }
        void scpd(HttpExchange exchange)throws IOException {
            String kind=exchange.getRequestURI().getPath().substring("/scpd/".length());hit("scpd."+kind);
            StringBuilder xml=new StringBuilder("<scpd><actionList>");
            for(String action:Arrays.asList("TracksMax","IdArray","ReadList","Insert","DeleteId","TransportState","Id","Pause","Play","Stop","SeekId","SeekSecondAbsolute","Repeat","Shuffle","SetRepeat","SetShuffle","SourceIndex","SourceXml","SetSourceIndex","Time","Volume","SetVolumeNoUnmute","VolumeIncNoUnmute","VolumeDecNoUnmute","GetServiceStatus")){
                xml.append("<action><name>").append(action).append("</name><argumentList>");
                List<String> inputs=action.equals("Insert")?Arrays.asList("AfterId","Uri","Metadata"):action.equals("ReadList")?Collections.singletonList("IdList"):Arrays.asList("DeleteId","SeekId","SeekSecondAbsolute","SetRepeat","SetShuffle","SetSourceIndex","SetVolumeNoUnmute").contains(action)?Collections.singletonList("Value"):Collections.emptyList();
                for(String input:inputs)xml.append("<argument><name>").append(input).append("</name><direction>in</direction></argument>");
                xml.append("</argumentList></action>");
            }
            send(exchange,xml.append("</actionList></scpd>").toString());
        }
        synchronized void control(HttpExchange exchange)throws IOException {
            try {
                String action=exchange.getRequestHeaders().getFirst("SOAPAction").split("#")[1].replace("\"","");hit(action);
                org.w3c.dom.Element request=OpenHome.xml(exchange.getRequestBody().readAllBytes()).getDocumentElement();String values;
                switch(action){
                    case "IdArray": {
                        byte[] ids=new byte[queue.size()*4];for(int i=0;i<queue.size();i++){long id=queue.get(i);for(int b=0;b<4;b++)ids[i*4+b]=(byte)(id>>>(24-b*8));}
                        values="<Array>"+Base64.getEncoder().encodeToString(ids)+"</Array>";break;
                    }
                    case "ReadList": {
                        StringBuilder list=new StringBuilder("<TrackList>");
                        for(String id:OpenHome.text(request,"IdList").split(" "))if(!id.isEmpty()){
                            long number=Long.parseLong(id);LinnQueueTransaction.Track track=tracks.get(number);
                            list.append("<Entry><Id>").append(number).append("</Id><Uri>").append(OpenHome.escape(track.uri)).append("</Uri><Metadata>").append(OpenHome.escape(track.metadata)).append("</Metadata></Entry>");
                        }
                        values="<TrackList>"+OpenHome.escape(list.append("</TrackList>").toString())+"</TrackList>";break;
                    }
                    case "Insert": {
                        long after=Long.parseLong(OpenHome.text(request,"AfterId")),id=next++;
                        queue.add(after==0?0:queue.indexOf(after)+1,id);tracks.put(id,new LinnQueueTransaction.Track("",OpenHome.text(request,"Uri"),OpenHome.text(request,"Metadata")));
                        if(loseInsert){loseInsert=false;exchange.close();return;}values="<NewId>"+id+"</NewId>";break;
                    }
                    case "DeleteId": {
                        long id=Long.parseLong(OpenHome.text(request,"Value"));queue.remove(id);tracks.remove(id);
                        if(loseDelete){loseDelete=false;exchange.close();return;}values="";break;
                    }
                    case "SourceXml": values="<Value>"+OpenHome.escape("<SourceList><Source><Name>Renamed</Name><Type>Playlist</Type></Source></SourceList>")+"</Value>";break;
                    case "SeekId": pendingTrack=Long.parseLong(OpenHome.text(request,"Value"));selected=42;trackPolls=0;seconds=0;playing=true;values="";break;
                    case "Id": if(pendingTrack!=0 && ++trackPolls>=3)selected=pendingTrack;values="<Value>"+selected+"</Value>";break;
                    case "Pause": if(pendingTrack!=0 && selected!=pendingTrack)premature=true;playing=false;values="";break;
                    case "Play": playing=true;values="";break;
                    case "SeekSecondAbsolute": if(selected!=pendingTrack)premature=true;seconds=Integer.parseInt(OpenHome.text(request,"Value"));values="";break;
                    case "Time": values="<Seconds>"+seconds+"</Seconds><Duration>180</Duration>";break;
                    case "TransportState": values="<Value>"+(playing?"Playing":"Paused")+"</Value>";break;
                    case "SourceIndex": values="<Value>0</Value>";break;
                    default: values="<Value>1000</Value>";
                }
                send(exchange,"<Envelope><"+action+"Response>"+values+"</"+action+"Response></Envelope>");
            }catch(Exception failure){exchange.close();throw new IOException(failure);}
        }
        synchronized void reset(List<LinnQueueTransaction.Track> items){queue.clear();tracks.clear();for(int i=0;i<items.size();i++){long id=i+1;queue.add(id);tracks.put(id,items.get(i));}loseInsert=false;loseDelete=false;}
        public void close(){server.stop(0);worker.shutdownNow();}
    }
    static LinnQueueTransaction.Track track(String key,int id){return new LinnQueueTransaction.Track(key,"tidal://track?version=2&trackId="+id+"&token=fixture","<DIDL-Lite><title>"+id+"</title></DIDL-Lite>");}
    static List<String> keys(List<LinnQueueTransaction.Track> tracks){List<String> keys=new ArrayList<>();for(LinnQueueTransaction.Track track:tracks)keys.add(track.key);return keys;}
    static void queueChecks(Fixture fixture,OpenHome device)throws Exception {
        List<LinnQueueTransaction.Track> original=Arrays.asList(track("occurrence-a",7),track("occurrence-b",8),track("occurrence-c",7));
        fixture.reset(original);List<Long> ids=device.ids();int inserts=fixture.count("Insert"),deletes=fixture.count("DeleteId");
        LinnQueueTransaction unchanged=new LinnQueueTransaction(device,original,keys(original),ids,3);
        unchanged.stage(() -> true,ignored -> {});unchanged.commit(() -> true);
        check(fixture.count("Insert")==inserts && fixture.count("DeleteId")==deletes,"Identical/full queue was restaged");
        check(unchanged.remote.equals(ids),"Duplicate title occurrences lost their IDs");
        List<LinnQueueTransaction.Track> appended=new ArrayList<>(original);appended.add(track("occurrence-d",9));
        LinnQueueTransaction append=new LinnQueueTransaction(device,appended,keys(original),ids,1000);
        fixture.loseInsert=true;append.stage(() -> true,ignored -> {});append.commit(() -> true);
        check(fixture.count("Insert")==inserts+1 && fixture.count("DeleteId")==deletes,"Append replaced old entries or retried a write");
        List<LinnQueueTransaction.Track> reordered=Arrays.asList(appended.get(2),appended.get(0),appended.get(1),appended.get(3));
        LinnQueueTransaction reorder=new LinnQueueTransaction(device,reordered,keys(appended),new ArrayList<>(append.remote),1000);
        reorder.stage(() -> true,ignored -> {});reorder.commit(() -> true);
        check(reorder.retained.size()==3 && reorder.inserted.size()==1,"Reorder did not retain the longest occurrence order");
        check(device.ids().equals(reorder.remote),"Reordered device IDs were not confirmed");
        fixture.reset(original);ids=device.ids();
        List<LinnQueueTransaction.Track> replacements=Arrays.asList(track("new-a",10),track("new-b",11));
        LinnQueueTransaction cancelled=new LinnQueueTransaction(device,replacements,Collections.emptyList(),Collections.emptyList(),1000);
        AtomicBoolean current=new AtomicBoolean(true);
        try{cancelled.stage(current::get,count -> current.set(false));throw new AssertionError("Cancellation ignored");}catch(IOException expected){}
        cancelled.rollback();check(device.ids().equals(ids),"Cancellation changed the original queue");
        LinnQueueTransaction partial=new LinnQueueTransaction(device,replacements,Collections.emptyList(),Collections.emptyList(),1000);
        partial.stage(() -> true,ignored -> {});AtomicInteger checks=new AtomicInteger();
        try{partial.commit(() -> checks.incrementAndGet()==1);throw new AssertionError("Commit cancellation ignored");}catch(IOException expected){}
        Map<Long,Long> restored=partial.rollback();
        check(device.ids().equals(Arrays.asList(restored.get(1L),2L,3L)),"Partially committed original order was not restored");
        fixture.reset(original);LinnQueueTransaction foreign=new LinnQueueTransaction(device,replacements,Collections.emptyList(),Collections.emptyList(),1000);
        foreign.stage(() -> true,ignored -> {});fixture.queue.add(999L);fixture.tracks.put(999L,track("foreign",12));deletes=fixture.count("DeleteId");
        try{foreign.commit(() -> true);throw new AssertionError("Foreign edit accepted");}catch(OpenHome.Fault expected){check(expected.kind==OpenHome.Failure.CONFLICT,"Foreign edit classification");}
        try{foreign.rollback();throw new AssertionError("Foreign queue destroyed");}catch(OpenHome.Fault expected){}
        check(fixture.count("DeleteId")==deletes,"Rollback deleted another controller's queue");
        fixture.reset(original);LinnQueueTransaction uncertain=new LinnQueueTransaction(device,replacements,Collections.emptyList(),Collections.emptyList(),1000);
        uncertain.stage(() -> true,ignored -> {});fixture.loseDelete=true;
        try{uncertain.commit(() -> true);throw new AssertionError("Lost Delete accepted");}catch(OpenHome.Fault expected){check(expected.kind==OpenHome.Failure.UNCERTAIN,"Lost Delete classification");}
        deletes=fixture.count("DeleteId");try{uncertain.rollback();throw new AssertionError("Uncertain rollback accepted");}catch(IOException expected){}
        check(fixture.count("DeleteId")==deletes,"Uncertain write caused more destructive writes");
        fixture.reset(original);boolean capacity=false;
        try{new LinnQueueTransaction(device,Arrays.asList(original.get(0),track("new",22),original.get(2)),keys(original),device.ids(),3);}catch(IllegalArgumentException expected){capacity=true;}
        check(capacity,"Reversible staging exceeded capacity");
    }
    static void discoveryChecks(InetAddress address,String location)throws Exception {
        ExecutorService worker=Executors.newFixedThreadPool(2);
        try(DatagramSocket responder=new DatagramSocket(new InetSocketAddress(address,0));LinnDiscovery scan=new LinnDiscovery()){
            CountDownLatch found=new CountDownLatch(1);AtomicInteger count=new AtomicInteger();
            Future<?> replies=worker.submit(() -> {try{
                DatagramPacket query=new DatagramPacket(new byte[2048],2048);responder.receive(query);
                for(String url:Arrays.asList("http://8.8.8.8/unsafe",location,location)){
                    byte[] reply=bytes("HTTP/1.1 200 OK\r\nLOCATION: "+url+"\r\nCACHE-CONTROL: max-age=60\r\nBOOTID.UPNP.ORG: 2\r\n\r\n");
                    responder.send(new DatagramPacket(reply,reply.length,query.getAddress(),query.getPort()));
                }
            }catch(Exception failure){throw new RuntimeException(failure);}});
            Future<?> searching=worker.submit(() -> {try{scan.search(address.getHostAddress(),responder.getLocalPort(),3000,result -> {count.incrementAndGet();found.countDown();});}catch(Exception failure){throw new RuntimeException(failure);}});
            check(found.await(1,TimeUnit.SECONDS),"Discovery waited for the entire SSDP window");
            check(!searching.isDone(),"Fixture did not exercise an active search");scan.close();searching.get(1,TimeUnit.SECONDS);replies.get(1,TimeUnit.SECONDS);
            check(count.get()==1,"SSDP location was not deduplicated");
            check(LinnDiscovery.parse("HTTP/1.1 200 OK\r\nLOCATION: http://192.168.99.99/elsewhere\r\n\r\n",address)==null,"SSDP redirect to another host accepted");
        }finally{worker.shutdownNow();}
    }
    static String notify(String callback,String sid,int sequence,String body,InetAddress sender)throws Exception {
        URL url=new URL(callback);try(Socket socket=new Socket()){
            socket.bind(new InetSocketAddress(sender,0));
            socket.connect(new InetSocketAddress(url.getHost(),url.getPort()));
            socket.setSoTimeout(2000);byte[] payload=bytes(body);
            socket.getOutputStream().write(bytes("NOTIFY /events HTTP/1.1\r\nSID: "+sid+"\r\nSEQ: "+sequence+"\r\nNT: upnp:event\r\nNTS: upnp:propchange\r\nContent-Length: "+payload.length+"\r\n\r\n"));
            socket.getOutputStream().write(payload);return LinnEvents.line(socket.getInputStream());
        }
    }
    static String eventBody(){return "<e:propertyset xmlns:e='urn:schemas-upnp-org:event-1-0'><e:property><TransportState>Paused</TransportState></e:property><e:property><Id>3</Id></e:property></e:propertyset>";}
    static void eventChecks(Fixture fixture,OpenHome device)throws Exception {
        InetAddress sender=InetAddress.getByName(new URL(device.location).getHost());
        List<String> traces=new CopyOnWriteArrayList<>();OpenHome.trace=traces::add;
        fixture.initialEventBeforeReply=true;
        List<LinnEvent> received=new CopyOnWriteArrayList<>();LinnEvents events=new LinnEvents(device,() -> {},received::add);events.start();
        try{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while((events.subscriptions.size()!=4 || received.isEmpty()) && System.nanoTime()<end)Thread.sleep(10);
            check(events.subscriptions.size()==4 && received.size()==1,"Initial event was lost before subscription SID publication");
            String body=eventBody();
            String reply=notify(events.callback,"uuid:fixture-Playlist",1,body,sender);
            check(reply.contains("200"),"Valid event rejected: "+reply+" "+traces);
            check(received.size()==2 && received.get(1).values.get("Id").equals("3") && events.healthy(),"Event payload not delivered");
            notify(events.callback,"uuid:fixture-Playlist",1,body,sender);check(received.size()==2,"Duplicate event sequence delivered");
            notify(events.callback,"uuid:fixture-Playlist",0,body,sender);check(received.size()==2,"Stale event sequence delivered");
            check(notify(events.callback,"uuid:unknown",2,body,sender).contains("412") && received.size()==2,"Unknown SID accepted");
            boolean invalid=false;try{LinnEvent.parse("Playlist",bytes(body.replace("Paused","InventedState")));}catch(IOException expected){invalid=true;}
            check(invalid,"Invalid transport value accepted");
        }finally{events.close();}
    }
    static void cacheAndCommands(){
        AtomicLong clock=new AtomicLong();LinnCache<String,String> cache=new LinnCache<>(2,10,clock::get);
        cache.put("a","one");cache.put("b","two");check(cache.get("a").equals("one"),"Cache lookup");cache.put("c","three");check(cache.get("b")==null,"Cache bound/LRU");
        clock.set(10);check(cache.get("a")==null,"Expired cache used");cache.put("d","four");cache.clear();check(cache.get("d")==null,"Network cache invalidation");
        List<Runnable> pending=new ArrayList<>();List<String> sent=new ArrayList<>();LinnCommandQueue commands=new LinnCommandQueue(pending::add);
        commands.execute("seek",() -> sent.add("old seek"));commands.execute("volume",() -> sent.add("volume"));commands.execute("seek",() -> sent.add("new seek"));pending.add(() -> sent.add("pause"));
        for(Runnable command:pending)command.run();check(sent.equals(Arrays.asList("volume","new seek","pause")),"Pending absolute controls were not coalesced/ordered");
    }
    static void selectionChecks(Fixture fixture,OpenHome device)throws Exception {
        ScheduledExecutorService controls=Executors.newSingleThreadScheduledExecutor();
        AtomicReference<Exception> failure=new AtomicReference<>();CountDownLatch complete=new CountDownLatch(1),unblocked=new CountDownLatch(1);
        try(LinnTrackSelection selection=new LinnTrackSelection(controls,() -> device,() -> true,complete::countDown,failure::set)){
            selection.select(3,0,true);check(selection.play(false) && selection.seek(7000),"Pending stream intent not updated");
            controls.execute(unblocked::countDown);
            check(unblocked.await(1,TimeUnit.SECONDS),"Stream confirmation blocked the control lane");
            check(complete.await(3,TimeUnit.SECONDS) && failure.get()==null,"Async selected stream not confirmed: "+failure.get());
            check(fixture.selected==3 && !fixture.playing && fixture.seconds==7 && !fixture.premature,"Pause/seek overtook selected ID");
        }
        CountDownLatch latest=new CountDownLatch(1);int seeks=fixture.count("SeekSecondAbsolute");
        try(LinnTrackSelection selection=new LinnTrackSelection(controls,() -> device,() -> true,latest::countDown,failure::set)){
            selection.select(1,0,true);selection.select(2,0,true);
            check(latest.await(3,TimeUnit.SECONDS) && failure.get()==null && fixture.selected==2,"Latest track target lost");
            check(fixture.count("SeekSecondAbsolute")==seeks,"Unnecessary zero seek issued");
        }
        CountDownLatch seeked=new CountDownLatch(1);int selections=fixture.count("SeekId"),pauses=fixture.count("Pause");fixture.seconds=10;
        try(LinnTrackSelection selection=new LinnTrackSelection(controls,() -> device,() -> true,seeked::countDown,failure::set)){
            selection.seekCurrent(2,0,false);
            check(seeked.await(3,TimeUnit.SECONDS) && failure.get()==null && fixture.seconds==0,"Current stream zero seek not applied");
            check(fixture.count("SeekId")==selections && fixture.count("Pause")==pauses && fixture.playing,"Seek restarted the stream or changed its transport");
        }finally{controls.shutdownNow();}
    }
    public static void main(String[] args)throws Exception {
        cacheAndCommands();InetAddress address=lan();
        try(Fixture fixture=new Fixture(address)){
            OpenHome.invalidateDescriptions();OpenHome device=OpenHome.cached(fixture.base+"/device");
            check(fixture.count("scpd.Playlist")==0,"Discovery eagerly downloaded SCPD");
            check(OpenHome.cached(fixture.base+"/device")==device && fixture.count("description")==1,"Descriptor cache missed");
            device.ids();new OpenHome(fixture.base+"/device").ids();check(fixture.count("scpd.Playlist")==1,"SCPD cache missed");
            device.playlistIndex();device.playlistSource();check(fixture.count("SourceXml")==1,"SourceXml was repeatedly fetched");
            device.invalidateSources();device.playlistIndex();check(fixture.count("SourceXml")==2,"Source configuration was not invalidated");
            queueChecks(fixture,device);discoveryChecks(address,fixture.base+"/device");selectionChecks(fixture,device);eventChecks(fixture,device);
            OpenHome.invalidateDescriptions();OpenHome.cached(fixture.base+"/device").ids();check(fixture.count("scpd.Playlist")==2,"Network change retained stale capabilities");
        }
        System.out.println("Linn performance fixtures passed: incremental discovery/cancel, caches, queue reuse/delta/rollback/conflicts, event payload/SID/sequence, coalesced controls");
    }
}
