/* GPL-3.0-or-later. Incremental SSDP with an explicitly cancellable socket. */
package dev.nino.squareglass;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class LinnDiscovery implements AutoCloseable {
    static final class Result {
        final String location,bootId,configId,uuid;
        final long leaseNanos;
        Result(String location,String bootId,String configId,String uuid,long leaseNanos) {
            this.location=location;this.bootId=bootId;this.configId=configId;this.uuid=uuid;this.leaseNanos=leaseNanos;
        }
    }
    private volatile boolean closed;
    private volatile DatagramSocket socket;

    void search(String host,int millis,Consumer<Result> found)throws Exception {search(host,1900,millis,found);}
    void search(String host,int port,int millis,Consumer<Result> found)throws Exception {
        if(closed)return;
        InetAddress target=InetAddress.getByName(host==null?"239.255.255.250":host);
        if(host!=null && (!target.isSiteLocalAddress() || target.isLoopbackAddress()))
            throw new java.io.IOException("Enter a local network IP address");
        try(DatagramSocket scanSocket=new DatagramSocket()) {
            socket=scanSocket;if(closed)return;
            if(OpenHome.network!=null)OpenHome.network.bind(scanSocket,target);
            byte[] query=("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: urn:av-openhome-org:service:Playlist:1\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
            scanSocket.send(new DatagramPacket(query,query.length,target,port));
            long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
            Set<String> seen=new HashSet<>();byte[] buffer=new byte[8192];
            while(!closed && System.nanoTime()<end) {
                scanSocket.setSoTimeout((int)Math.max(1,TimeUnit.NANOSECONDS.toMillis(end-System.nanoTime())));
                DatagramPacket reply=new DatagramPacket(buffer,buffer.length);
                try{scanSocket.receive(reply);}catch(SocketTimeoutException expired){break;}
                if(reply.getLength()==buffer.length)continue;
                try {
                    Result result=parse(new String(reply.getData(),0,reply.getLength(),StandardCharsets.ISO_8859_1),reply.getAddress());
                    if(result!=null && seen.add(result.location))found.accept(result);
                }catch(Exception invalid){/* Ignore an invalid response without losing other renderers. */}
            }
        }catch(SocketException stopped){if(!closed)throw stopped;}
        finally{socket=null;}
    }
    static Result parse(String raw,InetAddress sender)throws Exception {
        if(!raw.startsWith("HTTP/1.1 200 "))return null;
        Map<String,String> headers=new HashMap<>();
        for(String line:raw.split("\r\n")){int at=line.indexOf(':');if(at>0)headers.put(line.substring(0,at).trim().toLowerCase(Locale.ROOT),line.substring(at+1).trim());}
        String location=headers.get("location");if(location==null)return null;
        URL endpoint=OpenHome.local(location);
        if(!InetAddress.getByName(endpoint.getHost()).equals(sender))return null;
        long seconds=30;
        java.util.regex.Matcher maxAge=java.util.regex.Pattern.compile("(?i)max-age\\s*=\\s*(\\d+)").matcher(headers.getOrDefault("cache-control",""));
        if(maxAge.find())try{seconds=Math.max(5,Math.min(300,Long.parseLong(maxAge.group(1))));}catch(NumberFormatException ignored){}
        String uuid=headers.getOrDefault("usn","").split("::",2)[0];if(!uuid.startsWith("uuid:") || uuid.length()>160)uuid="";
        return new Result(location,headers.getOrDefault("bootid.upnp.org",""),headers.getOrDefault("configid.upnp.org",""),uuid,TimeUnit.SECONDS.toNanos(seconds));
    }
    public void close(){closed=true;DatagramSocket active=socket;if(active!=null)active.close();}
}
