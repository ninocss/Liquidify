package dev.nino.squareglass;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Bounded in-app GENA listener; polling remains a fallback when callbacks are blocked. */
final class LinnEvents implements AutoCloseable {
    final OpenHome transport;
    final Runnable changed;
    final ScheduledExecutorService executor=Executors.newScheduledThreadPool(2);
    final Map<String,String> subscriptions=new ConcurrentHashMap<>();
    final Map<String,Long> sequences=new ConcurrentHashMap<>();
    volatile boolean closed;
    ServerSocket server;
    String callback;
    LinnEvents(OpenHome transport,Runnable changed){this.transport=transport;this.changed=changed;}
    void start(){executor.execute(() -> {
        try {
            URL target=OpenHome.local(transport.services.get("Playlist").control);
            InetAddress local;
            try(DatagramSocket route=new DatagramSocket()){route.connect(InetAddress.getByName(target.getHost()),target.getPort());local=route.getLocalAddress();}
            server=new ServerSocket(0,4,local);server.setSoTimeout(1000);
            callback="http://"+local.getHostAddress()+":"+server.getLocalPort()+"/events";
            executor.execute(this::listen);
            renew();executor.scheduleWithFixedDelay(this::renew,120,120,TimeUnit.SECONDS);
        }catch(Exception ignored){/* Periodic SOAP reconciliation still runs. */}
    });}
    void renew(){
        for(String key:Arrays.asList("Playlist","Time","Volume","Product")) {
            if(closed)return;
            try {
                OpenHome.Service service=transport.services.get(key);if(service==null)continue;
                String sid=subscriptions.get(key);
                Map<String,String> headers=new LinkedHashMap<>();headers.put("TIMEOUT","Second-300");
                if(sid==null){headers.put("CALLBACK","<"+callback+">");headers.put("NT","upnp:event");}
                else headers.put("SID",sid);
                Map<String,String> result=request("SUBSCRIBE",service.event,headers);
                if(result.containsKey("sid"))subscriptions.put(key,result.get("sid"));
            }catch(Exception ignored){subscriptions.remove(key);}
        }
    }
    static String line(InputStream input) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();int value;
        while((value=input.read())!=-1){if(out.size()>=8192)throw new IOException("Header too long");if(value==10)break;if(value!=13)out.write(value);}
        if(value==-1 && out.size()==0)throw new EOFException();return out.toString("ISO-8859-1");
    }
    static Map<String,String> headers(InputStream input) throws IOException {
        Map<String,String> result=new HashMap<>();
        for(int count=0;count<64;count++){String line=line(input);if(line.isEmpty())return result;int at=line.indexOf(':');if(at>0)result.put(line.substring(0,at).trim().toLowerCase(Locale.ROOT),line.substring(at+1).trim());}
        throw new IOException("Too many headers");
    }
    static Map<String,String> request(String method,String endpoint,Map<String,String> headers) throws Exception {
        URL url=OpenHome.local(endpoint);int port=url.getPort()<0?80:url.getPort();
        try(Socket socket=new Socket()) {
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(url.getHost(),port),3000);socket.setSoTimeout(3000);
            StringBuilder request=new StringBuilder(method+" "+url.getFile()+" HTTP/1.1\r\nHOST: "+url.getHost()+":"+port+"\r\nConnection: close\r\n");
            for(Map.Entry<String,String> header:headers.entrySet())request.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            socket.getOutputStream().write(request.append("\r\n").toString().getBytes(StandardCharsets.US_ASCII));
            InputStream in=new BufferedInputStream(socket.getInputStream());String status=line(in);
            if(!status.matches("HTTP/1\\.[01] 200.*"))throw new IOException("Event subscription rejected");return headers(in);
        }
    }
    void listen(){
        while(!closed)try(Socket socket=server.accept()) {
            socket.setSoTimeout(2000);InputStream in=new BufferedInputStream(socket.getInputStream());String first=line(in);Map<String,String> values=headers(in);
            String sid=values.get("sid");boolean valid=first.equals("NOTIFY /events HTTP/1.1") && subscriptions.containsValue(sid)
                && "upnp:event".equals(values.get("nt")) && "upnp:propchange".equals(values.get("nts"));
            int size=Integer.parseInt(values.getOrDefault("content-length","0"));if(size<0 || size>131072)valid=false;
            if(valid) {
                byte[] body=new byte[size];int offset=0,n;while(offset<size && (n=in.read(body,offset,size-offset))>0)offset+=n;
                if(offset!=size)throw new EOFException();OpenHome.xml(body);
                long seq=Long.parseUnsignedLong(values.getOrDefault("seq","0"));Long previous=sequences.get(sid);
                if(previous==null || LinnQueuePlan.sequenceNewer(previous,seq)){sequences.put(sid,seq);changed.run();}
            }
            socket.getOutputStream().write((valid?"HTTP/1.1 200 OK\r\n":"HTTP/1.1 412 Precondition Failed\r\n").concat("Content-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        }catch(SocketTimeoutException ignored){}catch(Exception ignored){}
    }
    public synchronized void close(){
        if(closed)return;
        closed=true;try{if(server!=null)server.close();}catch(IOException ignored){}
        // Unsubscribe best-effort off the main thread; never stops playback.
        executor.execute(() -> {for(Map.Entry<String,String> entry:subscriptions.entrySet())try{
            request("UNSUBSCRIBE",transport.services.get(entry.getKey()).event,Collections.singletonMap("SID",entry.getValue()));
        }catch(Exception ignored){}executor.shutdown();});
    }
}
