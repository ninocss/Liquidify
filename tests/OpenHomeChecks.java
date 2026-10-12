package dev.nino.squareglass;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class OpenHomeChecks {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args)throws Exception {
        check(OpenHome.escape("<&\"'>").equals("&lt;&amp;&quot;&apos;&gt;"));
        check(OpenHome.text(OpenHome.xml("<root xmlns='urn:test'><value>Größe</value></root>".getBytes(StandardCharsets.UTF_8)).getDocumentElement(),"value").equals("Größe"));
        for(String xml:new String[]{"<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///secret'>]><a>&x;</a>","<!ENTITY x 'a'><a/>"}){
            boolean rejected=false;try{OpenHome.xml(xml.getBytes(StandardCharsets.UTF_8));}catch(IOException expected){rejected=true;}check(rejected);
        }
        for(String url:new String[]{"https://192.168.1.2/a","http://user:password@192.168.1.2/a","http://127.0.0.1/a","http://8.8.8.8/a"}){
            boolean rejected=false;try{OpenHome.local(url);}catch(IOException expected){rejected=true;}check(rejected);
        }
        check(LinnEvents.headers(new ByteArrayInputStream("SID: uuid:test\r\nSEQ: 7\r\n\r\n".getBytes())).get("sid").equals("uuid:test"));
        boolean rejected=false;try{LinnEvents.line(new ByteArrayInputStream(new byte[8200]));}catch(IOException expected){rejected=true;}check(rejected);
        rejected=false;try{OpenHome.read(new ByteArrayInputStream(new byte[2]),new ByteArrayOutputStream(),3);}catch(EOFException expected){rejected=true;}check(rejected);
        if(args.length==1){
            OpenHome device=new OpenHome(args[0]);
            check(device.services.containsKey("Playlist"));
            java.util.List<Long> ids=device.ids();
            check(device.readTracks(ids.subList(0,Math.min(2,ids.size()))).size()==Math.min(2,ids.size()));
            System.out.println("Actual Java LAN client: mute="+device.read("Volume","Mute")+", queue="+device.ids().size());
        }
    }
}
