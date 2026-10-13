package dev.nino.squareglass;

import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real HTTP fault injection; production endpoint validation remains enabled. */
public final class LinnFaultChecks {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    static void send(HttpExchange e,int status,String s)throws IOException{
        byte[] b=bytes(s);e.sendResponseHeaders(status,b.length);e.getResponseBody().write(b);e.close();
    }
    static String response(String action,String values){return "<Envelope><"+action+"Response>"+values+"</"+action+"Response></Envelope>";}
    public static void main(String[] args)throws Exception{
        InetAddress lan=null;
        for(NetworkInterface n:Collections.list(NetworkInterface.getNetworkInterfaces()))
            for(InetAddress a:Collections.list(n.getInetAddresses()))if(a instanceof Inet4Address && a.isSiteLocalAddress())lan=a;
        if(lan==null)throw new AssertionError("Test requires a private LAN interface (does not contact a renderer)");
        HttpServer server=HttpServer.create(new InetSocketAddress(lan,0),0);
        String base="http://"+lan.getHostAddress()+":"+server.getAddress().getPort();
        AtomicInteger reads=new AtomicInteger(),writes=new AtomicInteger(),polls=new AtomicInteger();
        AtomicInteger inserts=new AtomicInteger(),queueSize=new AtomicInteger();
        AtomicInteger delay=new AtomicInteger();
        AtomicInteger selectionPolls=new AtomicInteger(),selected=new AtomicInteger(42),seconds=new AtomicInteger();
        boolean[] selecting={false},premature={false},paused={false};
        String uri="tidal://track?version=2&trackId=123&token=private",metadata="<DIDL-Lite/>";
        List<String> traces=new ArrayList<>();OpenHome.trace=traces::add;
        server.createContext("/fault",e -> send(e,500,"<Fault><errorCode>800</errorCode><errorDescription>secret-token@example.invalid</errorDescription></Fault>"));
        server.createContext("/retry",e -> {if(reads.incrementAndGet()<3)e.close();else send(e,200,"<ok/> ");});
        server.createContext("/write",e -> {writes.incrementAndGet();e.close();});
        server.createContext("/device",e -> send(e,200,"<root><device><UDN>uuid:test</UDN><friendlyName>Test</friendlyName><manufacturer>Linn</manufacturer><modelName>Majik</modelName><serviceList>"+
            "<service><serviceType>urn:av-openhome-org:service:Playlist:1</serviceType><controlURL>/control</controlURL><eventSubURL>/events</eventSubURL><SCPDURL>/scpd</SCPDURL></service>"+
            "<service><serviceType>urn:av-openhome-org:service:OAuth:1</serviceType><controlURL>/control</controlURL><eventSubURL>/events</eventSubURL><SCPDURL>/scpd</SCPDURL></service>"+
            "<service><serviceType>urn:av-openhome-org:service:Time:1</serviceType><controlURL>/control</controlURL><eventSubURL>/events</eventSubURL><SCPDURL>/scpd</SCPDURL></service></serviceList></device></root>"));
        server.createContext("/scpd",e -> send(e,200,"<scpd><actionList><action><name>Id</name></action><action><name>TransportState</name></action>"+
            "<action><name>Pause</name></action><action><name>Play</name></action><action><name>Time</name></action>"+
            "<action><name>SeekId</name><argumentList><argument><name>Value</name><direction>in</direction></argument></argumentList></action>"+
            "<action><name>SeekSecondAbsolute</name><argumentList><argument><name>Value</name><direction>in</direction></argument></argumentList></action>"+
            "<action><name>IdArray</name></action><action><name>ReadList</name><argumentList><argument><name>IdList</name><direction>in</direction></argument></argumentList></action>"+
            "<action><name>Insert</name><argumentList><argument><name>AfterId</name><direction>in</direction></argument><argument><name>Uri</name><direction>in</direction></argument><argument><name>Metadata</name><direction>in</direction></argument></argumentList></action></actionList></scpd>"));
        server.createContext("/control",e -> {
            if(delay.get()>0)try{Thread.sleep(delay.get());}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
            String action=e.getRequestHeaders().getFirst("SOAPAction");
            if(action.contains("#Insert")){inserts.incrementAndGet();queueSize.incrementAndGet();e.close();}
            else if(action.contains("#IdArray")){
                byte[] ids=new byte[queueSize.get()*4];for(int i=0;i<queueSize.get();i++)ids[i*4+3]=(byte)(42+i);
                send(e,200,response("IdArray","<Array>"+Base64.getEncoder().encodeToString(ids)+"</Array>"));
            }
            else if(action.contains("#ReadList")){
                String body=new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
                String tracks="<TrackList>";
                for(int i=0;i<queueSize.get();i++)if(body.contains(Integer.toString(42+i)))tracks+="<Entry><Id>"+(42+i)+"</Id><Uri>"+OpenHome.escape(uri)+"</Uri><Metadata>"+OpenHome.escape(metadata)+"</Metadata></Entry>";
                send(e,200,response("ReadList","<TrackList>"+OpenHome.escape(tracks+"</TrackList>")+"</TrackList>"));
            }
            else if(action.contains("#SeekId")){
                selecting[0]=true;paused[0]=false;selectionPolls.set(0);selected.set(42);
                send(e,200,response("SeekId",""));
            }
            else if(action.contains("#Pause")){
                if(selecting[0] && selected.get()!=99)premature[0]=true;
                paused[0]=true;send(e,200,response("Pause",""));
            }
            else if(action.contains("#Play")){paused[0]=false;send(e,200,response("Play",""));}
            else if(action.contains("#SeekSecondAbsolute")){
                if(selected.get()!=99)premature[0]=true;
                seconds.set(7);send(e,200,response("SeekSecondAbsolute",""));
            }
            else if(action.contains("#Time"))send(e,200,response("Time","<Seconds>"+seconds.get()+"</Seconds>"));
            else if(action.contains("#Id")){
                if(selecting[0] && !premature[0] && selectionPolls.incrementAndGet()>=3)selected.set(99);
                send(e,200,response("Id","<Value>"+selected.get()+"</Value>"));
            }
            else send(e,200,response("TransportState","<Value>"+(paused[0]?"Paused":polls.incrementAndGet()<3?"Buffering":"Playing")+"</Value>"));
        });
        server.start();
        try{
            try{OpenHome.fetch(base+"/fault",bytes("x"),"Playlist#Play");throw new AssertionError("Fault missing");}
            catch(OpenHome.Fault f){check(f.status==500 && f.code.equals("800") && f.kind==OpenHome.Failure.REJECTED,"Fault details");check(!f.getMessage().contains("secret"),"Secret leaked");}
            check(OpenHome.fetch(base+"/retry",null,null).length>0 && reads.get()==3,"Read retry budget");
            try{OpenHome.fetch(base+"/write",bytes("x"),"Playlist#Insert");throw new AssertionError("Uncertain write missing");}
            catch(OpenHome.Fault f){check(f.kind==OpenHome.Failure.UNCERTAIN && writes.get()==1,"Write was retried");}
            OpenHome device=new OpenHome(base+"/device");device.confirm(42,"Playing",2000);
            check(polls.get()==3,"Buffering not tolerated");
            try{device.confirm(99,"Playing",30);throw new AssertionError("Confirmation did not expire");}
            catch(OpenHome.Fault f){check(f.kind==OpenHome.Failure.TIMEOUT,"Confirmation timeout");}
            check(device.insertConfirmed(Collections.emptyList(),0,uri,metadata)==42,"Lost insert response reconciliation");
            check(device.insertConfirmed(Collections.singletonList(42L),42,uri,metadata)==43,"Duplicate track occurrence reconciliation");
            check(inserts.get()==2 && device.ids().equals(Arrays.asList(42L,43L)),"Duplicate insert after retry");
            queueSize.incrementAndGet(); // Simultaneous foreign insertion: never attribute it to our operation.
            try{device.insertConfirmed(Arrays.asList(42L,43L),43,uri,metadata);throw new AssertionError("Conflict accepted");}
            catch(OpenHome.Fault f){check(f.kind==OpenHome.Failure.UNCERTAIN,"Uncertain insertion conflict");}
            device.selectTrack(99,0,false);
            check(selected.get()==99 && paused[0] && !premature[0],"Pause overtook asynchronous track selection");
            device.selectTrack(99,0,true);
            check(selected.get()==99 && !paused[0] && !premature[0],"Playing handoff failed");
            device.selectTrack(99,7000,false);
            check(seconds.get()==7 && paused[0] && !premature[0],"Position seek overtook track selection");
            selecting[0]=false;selected.set(42);
            delay.set(300);long began=System.nanoTime();
            try{device.confirm(42,"Playing",60);throw new AssertionError("Socket deadline missing");}
            catch(OpenHome.Fault f){check(f.kind==OpenHome.Failure.TIMEOUT,"Socket timeout category");}
            check((System.nanoTime()-began)/1000000<1000,"Deadline exceeded by read retry");
            check(traces.stream().noneMatch(t -> t.contains("secret") || t.contains(base)),"Trace privacy");
            System.out.println("Linn fault server: SOAP, redaction, read retries, uncertain writes, buffering, deadline, lost insert, duplicate occurrences and concurrent insertion passed");
        }finally{server.stop(0);}
    }
}
