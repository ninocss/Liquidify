/* SquareGlass. GPL-3.0-or-later. LAN-only OpenHome transport. */
package dev.nino.squareglass;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.json.*;

final class OpenHome {
    interface NetworkBinding {
        void bind(Socket socket,InetAddress target) throws IOException;
        void bind(DatagramSocket socket,InetAddress target) throws IOException;
    }
    static volatile NetworkBinding network;
    interface Trace { void record(String message); }
    static volatile Trace trace=message -> {};
    static final ThreadLocal<String> attempt=new ThreadLocal<>();
    static final ThreadLocal<Long> deadline=new ThreadLocal<>();
    static int timeout(int maximum)throws SocketTimeoutException{
        Long end=deadline.get();if(end==null)return maximum;
        long remaining=TimeUnit.NANOSECONDS.toMillis(end-System.nanoTime());
        if(remaining<=0)throw new SocketTimeoutException("Linn operation deadline");
        return (int)Math.min(maximum,remaining);
    }
    enum Failure { UNREACHABLE, TIMEOUT, REJECTED, AUTH, CONFLICT, UNCERTAIN }
    static final class Fault extends IOException {
        final Failure kind; final int status; final String code;
        Fault(Failure kind,String action,int status,String code,String description,Throwable cause){
            super(action+": "+kind+(status==0?"":" (HTTP "+status+", SOAP "+code+")")+
                (description.isEmpty()?"":" — "+description),cause);
            this.kind=kind;this.status=status;this.code=code;
        }
    }
    // Device-provided strings are untrusted. Only known protocol descriptions enter logs/UI.
    static String safeDescription(String value){
        for(String known:Arrays.asList("Action Failed","Invalid Action","Invalid Args","Transition not available","Invalid Id","Buffer full","Not supported","Not authorized","Id not found","Playlist full","Index not found","Seek failed","Shuffle not currently possible"))
            if(known.equalsIgnoreCase(value.trim()))return known;
        return "Device action rejected";
    }
    static boolean readOnly(String action){
        if(action==null)return true;
        String name=action.substring(action.lastIndexOf('#')+1);
        return Arrays.asList("TracksMax","IdArray","ReadList","SourceIndex","SourceXml","TransportState","Id","Time","Volume","Mute","Shuffle","Repeat","GetServiceStatus").contains(name);
    }
    static final int LIMIT=1024*1024;
    final String uuid,name,model,location;
    final Map<String,Service> services=new HashMap<>();
    static final class Service {
        String type,control,event;
        Map<String,List<String>> inputs=new HashMap<>();
    }
    static String text(Element e,String tag) {
        NodeList list=e.getElementsByTagNameNS("*",tag);
        return list.getLength()==0?"":list.item(0).getTextContent();
    }
    static Document xml(byte[] body) throws Exception {
        String raw=new String(body,StandardCharsets.UTF_8);
        if(raw.toUpperCase(Locale.ROOT).contains("<!DOCTYPE") || raw.toUpperCase(Locale.ROOT).contains("<!ENTITY"))
            throw new IOException("Unsafe device XML");
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(body));
    }
    static URL local(String value) throws Exception {
        URL url=new URL(value);
        if(!url.getProtocol().equals("http") || url.getUserInfo()!=null)throw new IOException("Expected LAN HTTP endpoint");
        for(InetAddress address:InetAddress.getAllByName(url.getHost()))
            if(!address.isSiteLocalAddress() || address.isLoopbackAddress())throw new IOException("Non-LAN endpoint refused");
        return url;
    }
    static byte[] fetch(String url,byte[] body,String action) throws Exception {
        String label=action==null?"Description":action.replace("urn:av-openhome-org:service:","").replace('#','.');
        long began=System.nanoTime();
        for(int retry=0;;retry++)try{
            byte[] result=fetchOnce(url,body,action);
            trace.record("attempt="+attempt.get()+" action="+label+" ms="+(System.nanoTime()-began)/1000000+" status=200 retries="+retry);
            return result;
        }catch(IOException failure){
            boolean transientFailure=failure instanceof SocketTimeoutException || failure instanceof ConnectException || failure instanceof NoRouteToHostException || failure instanceof EOFException || failure instanceof SocketException;
            if(transientFailure && readOnly(action) && retry<2 && (deadline.get()==null || deadline.get()>System.nanoTime()+TimeUnit.SECONDS.toNanos(1))){Thread.sleep(250L*(retry+1));continue;}
            Fault fault=failure instanceof Fault?(Fault)failure:new Fault(!readOnly(action)?Failure.UNCERTAIN:failure instanceof SocketTimeoutException?Failure.TIMEOUT:Failure.UNREACHABLE,label,0,"","",failure);
            trace.record("attempt="+attempt.get()+" action="+label+" ms="+(System.nanoTime()-began)/1000000+" error="+fault.getMessage());
            throw fault;
        }
    }
    static byte[] fetchOnce(String url,byte[] body,String action) throws Exception {
        // Native UPnP is LAN HTTP. Keep it scoped here instead of weakening the
        // application's network security policy for unrelated web traffic.
        URL endpoint=local(url);int port=endpoint.getPort()<0?80:endpoint.getPort();
        InetAddress address=InetAddress.getByName(endpoint.getHost());
        if(!address.isSiteLocalAddress() || address.isLoopbackAddress())throw new IOException("Non-LAN endpoint refused");
        try(Socket connection=new Socket()) {
            if(network!=null)network.bind(connection,address);
            connection.setTcpNoDelay(true);connection.connect(new InetSocketAddress(address,port),timeout(4000));connection.setSoTimeout(timeout(7000));
            String path=endpoint.getFile();if(path.isEmpty())path="/";
            String request=(body==null?"GET":"POST")+" "+path+" HTTP/1.1\r\nHOST: "+endpoint.getHost()+":"+port+"\r\nConnection: close\r\n";
            if(body!=null)request+="Content-Type: text/xml; charset=\"utf-8\"\r\nSOAPACTION: \""+action+"\"\r\nContent-Length: "+body.length+"\r\n";
            ByteArrayOutputStream wire=new ByteArrayOutputStream();wire.write((request+"\r\n").getBytes(StandardCharsets.US_ASCII));
            if(body!=null)wire.write(body);connection.getOutputStream().write(wire.toByteArray());
            InputStream in=new BufferedInputStream(connection.getInputStream());String status=LinnEvents.line(in);
            if(!status.matches("HTTP/1\\.[01] [0-9]{3}.*"))throw new IOException("Invalid HTTP status");
            int statusCode=Integer.parseInt(status.substring(9,12));
            Map<String,String> headers=LinnEvents.headers(in);ByteArrayOutputStream out=new ByteArrayOutputStream();
            if("chunked".equalsIgnoreCase(headers.get("transfer-encoding"))) {
                while(true) {
                    int length=Integer.parseInt(LinnEvents.line(in).split(";",2)[0].trim(),16);
                    if(length<0 || (long)out.size()+length>LIMIT)throw new IOException("Device response too large");
                    if(length==0)break;read(in,out,length);if(!LinnEvents.line(in).isEmpty())throw new IOException("Invalid HTTP chunk");
                }
            }else if(headers.containsKey("content-length")) {
                int length=Integer.parseInt(headers.get("content-length"));
                if(length<0 || length>LIMIT)throw new IOException("Device response too large");read(in,out,length);
            }else {
                byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>LIMIT)throw new IOException("Device response too large");out.write(buffer,0,n);}
            }
            byte[] response=out.toByteArray();
            if(statusCode!=200){
                String code="",description="Device action rejected";
                try{Element fault=xml(response).getDocumentElement();code=text(fault,"errorCode");if(!code.matches("[0-9]{1,6}"))code="";description=safeDescription(text(fault,"errorDescription"));}catch(Exception ignored){}
                throw new Fault(statusCode==401 || statusCode==403?Failure.AUTH:Failure.REJECTED,action==null?"Description":action.substring(action.lastIndexOf('#')+1),statusCode,code,description,null);
            }
            return response;
        }
    }
    static void read(InputStream in,ByteArrayOutputStream out,int count) throws IOException {
        byte[] buffer=new byte[8192];while(count>0){int n=in.read(buffer,0,Math.min(count,buffer.length));if(n<0)throw new EOFException();out.write(buffer,0,n);count-=n;}
    }
    OpenHome(String url) throws Exception {
        location=url;Document doc=xml(fetch(url,null,null));Element root=doc.getDocumentElement();
        String base=text(root,"URLBase");if(base.isEmpty())base=url;local(base);
        Element device=(Element)root.getElementsByTagNameNS("*","device").item(0);
        uuid=text(device,"UDN");name=text(device,"friendlyName");model=text(device,"modelName");
        if(uuid.isEmpty() || !text(device,"manufacturer").toLowerCase(Locale.ROOT).contains("linn"))
            throw new IOException("Not a Linn renderer");
        NodeList nodes=device.getElementsByTagNameNS("*","service");
        for(int i=0;i<nodes.getLength();i++) {
            Element node=(Element)nodes.item(i);String type=text(node,"serviceType");
            if(!type.startsWith("urn:av-openhome-org:service:"))continue;
            String key=type.split(":")[3];
            if(!Arrays.asList("Playlist","Product","Time","Info","Volume","OAuth").contains(key))continue;
            Service service=new Service();service.type=type;
            service.control=new URL(new URL(base),text(node,"controlURL")).toString();local(service.control);
            service.event=new URL(new URL(base),text(node,"eventSubURL")).toString();local(service.event);
            Document scpd=xml(fetch(new URL(new URL(base),text(node,"SCPDURL")).toString(),null,null));
            NodeList actions=scpd.getElementsByTagNameNS("*","action");
            for(int a=0;a<actions.getLength();a++) {
                Element action=(Element)actions.item(a);List<String> inputs=new ArrayList<>();
                NodeList arguments=action.getElementsByTagNameNS("*","argument");
                for(int b=0;b<arguments.getLength();b++) {
                    Element argument=(Element)arguments.item(b);
                    if("in".equals(text(argument,"direction")))inputs.add(text(argument,"name"));
                }
                service.inputs.put(text(action,"name"),inputs);
            }
            services.put(key,service);
        }
        if(!services.containsKey("Playlist") || !services.containsKey("OAuth"))throw new IOException("OpenHome TIDAL services unavailable");
    }
    static String escape(String value) {return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    Map<String,String> call(String kind,String action,String... arguments) throws Exception {
        Service service=services.get(kind);
        if(service==null || !service.inputs.containsKey(action))throw new IOException("Unsupported device action: "+kind+"."+action);
        List<String> inputs=service.inputs.get(action);
        if(inputs.size()!=arguments.length)throw new IOException("SOAP argument mismatch");
        StringBuilder body=new StringBuilder("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:")
            .append(action).append(" xmlns:u=\"").append(service.type).append("\">");
        for(int i=0;i<arguments.length;i++)body.append('<').append(inputs.get(i)).append('>').append(escape(arguments[i]))
            .append("</").append(inputs.get(i)).append('>');
        body.append("</u:").append(action).append("></s:Body></s:Envelope>");
        byte[] responseBytes=fetch(service.control,body.toString().getBytes(StandardCharsets.UTF_8),service.type+"#"+action);
        Document doc;
        try{doc=xml(responseBytes);}catch(Exception invalid){throw new Fault(readOnly(service.type+"#"+action)?Failure.REJECTED:Failure.UNCERTAIN,kind+"."+action,200,"","Invalid device response",invalid);}
        NodeList response=doc.getElementsByTagNameNS("*",action+"Response");
        if(response.getLength()!=1)throw new Fault(readOnly(service.type+"#"+action)?Failure.REJECTED:Failure.UNCERTAIN,kind+"."+action,200,"","Missing device response",null);
        Map<String,String> result=new HashMap<>();NodeList values=response.item(0).getChildNodes();
        for(int i=0;i<values.getLength();i++)if(values.item(i) instanceof Element)result.put(values.item(i).getLocalName(),values.item(i).getTextContent());
        return result;
    }
    String read(String service,String action) throws Exception {return call(service,action).get("Value");}
    void preflight() throws Exception {
        for(String kind:Arrays.asList("Playlist","Product","Time","Volume","OAuth"))
            if(!services.containsKey(kind))throw new IOException("Missing Linn service: "+kind);
        String[][] required={
            {"Playlist","TracksMax","IdArray","ReadList","Insert","DeleteId","TransportState","Id","Pause","Play","Stop","SeekId","SeekSecondAbsolute","Repeat","Shuffle","SetRepeat","SetShuffle"},
            {"Product","SourceIndex","SourceXml","SetSourceIndex"},{"Time","Time"},
            {"Volume","Volume","SetVolumeNoUnmute","VolumeIncNoUnmute","VolumeDecNoUnmute"},{"OAuth","GetServiceStatus"}};
        for(String[] group:required)for(int i=1;i<group.length;i++)
            if(!services.get(group[0]).inputs.containsKey(group[i]))throw new IOException("Unsupported Linn action: "+group[0]+"."+group[i]);
        tidalTokenId();
    }
    int playlistIndex()throws Exception{
        NodeList sources=xml(read("Product","SourceXml").getBytes(StandardCharsets.UTF_8)).getElementsByTagName("Source");
        for(int i=0;i<sources.getLength();i++)if("Playlist".equals(text((Element)sources.item(i),"Type")))return i;
        throw new IOException("Linn Playlist source unavailable");
    }
    boolean playlistSource()throws Exception{
        int index=Integer.parseInt(read("Product","SourceIndex"));
        NodeList sources=xml(read("Product","SourceXml").getBytes(StandardCharsets.UTF_8)).getElementsByTagName("Source");
        return index>=0 && index<sources.getLength() && "Playlist".equals(text((Element)sources.item(index),"Type"));
    }
    long insertConfirmed(List<Long> before,long after,String uri,String metadata) throws Exception {
        try{return Long.parseLong(call("Playlist","Insert",Long.toString(after),uri,metadata).get("NewId"));}
        catch(Fault failure){
            if(failure.kind!=Failure.UNCERTAIN)throw failure;
            List<Long> now=ids();List<Long> added=new ArrayList<>(now);added.removeAll(before);
            if(added.size()==1){
                long id=added.get(0);List<Long> expected=new ArrayList<>(before);int at=after==0?0:before.indexOf(after)+1;
                expected.add(at,id);
                Map<String,String> entry=readTracks(Collections.singletonList(id)).get(id);
                if(expected.equals(now) && uri.equals(entry.get("Uri")) && metadata.equals(entry.get("Metadata")))return id;
            }
            throw failure;
        }
    }
    void confirm(long id,String state,long timeoutMillis)throws Exception {
        long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        Long previous=deadline.get();deadline.set(previous==null?end:Math.min(previous,end));
        try{
        do{
            String actual=read("Playlist","Id"),transport=read("Playlist","TransportState");
            if(Long.toString(id).equals(actual) && state.equals(transport))return;
            Thread.sleep(250);
        }while(System.nanoTime()<end);
        throw new Fault(Failure.TIMEOUT,"Playback confirmation",0,"","Linn did not confirm the requested playback state",null);
        }finally{if(previous==null)deadline.remove();else deadline.set(previous);}
    }
    /** SeekId starts an asynchronous stream change; position/pause must target that stream. */
    void selectTrack(long id,int millis,boolean play)throws Exception {
        Long previous=deadline.get();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        deadline.set(previous==null?end:Math.min(previous,end));
        try {
            call("Playlist","SeekId",Long.toString(id));
            String last="";
            while(true){
                String actual=read("Playlist","Id"),state=read("Playlist","TransportState");
                String observed=actual+":"+state;
                if(!observed.equals(last)){trace.record("attempt="+attempt.get()+" awaiting_track target="+id+" actual="+actual+" state="+state);last=observed;}
                if(Long.toString(id).equals(actual))break;
                if(System.nanoTime()>=end)throw new Fault(Failure.TIMEOUT,"Track selection",0,"","Linn did not confirm the selected track",null);
                Thread.sleep(100);
            }
            // Zero is the new track's initial position. Seeking zero before its
            // stream exists can restart the previous stream on Linn firmware.
            if(millis>0)seekConfirmed(millis);
            if(!play)call("Playlist","Pause");
            else if(!"Playing".equals(read("Playlist","TransportState")))call("Playlist","Play");
            confirm(id,play?"Playing":"Paused",20000);
        }finally{if(previous==null)deadline.remove();else deadline.set(previous);}
    }
    void seekConfirmed(int millis)throws Exception {
        int seconds=Math.max(0,millis/1000);
        Long previous=deadline.get();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        deadline.set(previous==null?end:Math.min(previous,end));
        try{
            do {
                try{call("Playlist","SeekSecondAbsolute",Integer.toString(seconds));}
                catch(Fault failure){if(failure.kind!=Failure.UNCERTAIN && !(failure.kind==Failure.REJECTED && "803".equals(failure.code)))throw failure;}
                int actual=Integer.parseInt(call("Time","Time").get("Seconds"));
                if(Math.abs(actual-seconds)<=2)return;
                Thread.sleep(300);
            }while(System.nanoTime()<end);
            throw new Fault(Failure.TIMEOUT,"Position confirmation",0,"","Linn did not confirm the playback position",null);
        }finally{if(previous==null)deadline.remove();else deadline.set(previous);}
    }
    List<Long> ids() throws Exception {
        byte[] bytes=java.util.Base64.getDecoder().decode(call("Playlist","IdArray").get("Array"));
        if(bytes.length%4!=0)throw new IOException("Invalid queue IDs");
        List<Long> ids=new ArrayList<>();
        for(int i=0;i<bytes.length;i+=4)ids.add(((bytes[i]&255L)<<24)|((bytes[i+1]&255L)<<16)|((bytes[i+2]&255L)<<8)|(bytes[i+3]&255L));
        return ids;
    }
    Map<Long,Map<String,String>> readTracks(List<Long> ids) throws Exception {
        Map<Long,Map<String,String>> result=new LinkedHashMap<>();
        for(int from=0;from<ids.size();from+=32) {
            List<Long> batch=ids.subList(from,Math.min(ids.size(),from+32));
            StringBuilder argument=new StringBuilder();for(long id:batch)argument.append(id).append(' ');
            Element root=xml(call("Playlist","ReadList",argument.toString().trim()).get("TrackList").getBytes(StandardCharsets.UTF_8)).getDocumentElement();
            NodeList entries=root.getElementsByTagName("Entry");
            for(int i=0;i<entries.getLength();i++) {
                Element entry=(Element)entries.item(i);long id=Long.parseLong(text(entry,"Id"));
                if(!batch.contains(id) || result.containsKey(id))throw new IOException("Unexpected device queue entry");
                Map<String,String> track=new HashMap<>();track.put("Uri",text(entry,"Uri"));track.put("Metadata",text(entry,"Metadata"));result.put(id,track);
            }
            if(!result.keySet().containsAll(batch))throw new IOException("Device queue changed while reading");
        }
        return result;
    }
    String tidalTokenId() throws Exception {
        JSONObject status=new JSONObject(call("OAuth","GetServiceStatus").get("ServiceStatusJson"));
        JSONArray services=status.getJSONArray("services");
        for(int i=0;i<services.length();i++) {
            JSONObject service=services.getJSONObject(i);if(!"tidalhifi.com".equals(service.optString("id")))continue;
            for(String key:Arrays.asList("longLivedTokens","shortLivedTokens")) {
                JSONArray tokens=service.optJSONArray(key);if(tokens==null)continue;
                for(int j=0;j<tokens.length();j++)if(tokens.getJSONObject(j).optBoolean("isValid"))return tokens.getJSONObject(j).getString("id");
            }
        }
        throw new Fault(Failure.AUTH,"OAuth.GetServiceStatus",0,"","Sign in to TIDAL for this Majik in the Linn app, then retry",null);
    }
    static String tidalUri(int track,String token) throws Exception {
        if(track<=0 || token.isEmpty())throw new IOException("Invalid online TIDAL track");
        return "tidal://track?version=2&trackId="+track+"&token="+URLEncoder.encode(token,"UTF-8");
    }
    static Set<String> discover(int millis) throws Exception {
        return discover(null,millis);
    }
    static Set<String> discover(String host,int millis) throws Exception {
        Set<String> locations=new LinkedHashSet<>();
        try(DatagramSocket socket=new DatagramSocket()) {
            byte[] message=("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: urn:av-openhome-org:service:Playlist:1\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
            InetAddress target=InetAddress.getByName(host==null?"239.255.255.250":host);
            if(network!=null)network.bind(socket,target);
            if(host!=null && (!target.isSiteLocalAddress() || target.isLoopbackAddress()))throw new IOException("Enter a local network IP address");
            socket.send(new DatagramPacket(message,message.length,target,1900));
            long deadline=System.currentTimeMillis()+millis;byte[] buffer=new byte[65507];
            while(System.currentTimeMillis()<deadline) {
                socket.setSoTimeout((int)Math.max(1,deadline-System.currentTimeMillis()));
                DatagramPacket packet=new DatagramPacket(buffer,buffer.length);
                try{socket.receive(packet);}catch(SocketTimeoutException end){break;}
                String raw=new String(packet.getData(),0,packet.getLength(),StandardCharsets.ISO_8859_1);
                if(!raw.startsWith("HTTP/1.1 200"))continue;
                for(String line:raw.split("\r\n"))if(line.toLowerCase(Locale.ROOT).startsWith("location:")) {
                    String location=line.substring(9).trim();local(location);
                    // Do not follow an SSDP response to a different host.
                    if(InetAddress.getByName(new URL(location).getHost()).equals(packet.getAddress()))locations.add(location);
                }
            }
        }
        return locations;
    }
}
