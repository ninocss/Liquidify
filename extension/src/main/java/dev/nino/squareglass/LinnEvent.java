/* GPL-3.0-or-later. Only subscribed, validated OpenHome state enters the reducer. */
package dev.nino.squareglass;

import java.io.IOException;
import java.util.*;
import org.w3c.dom.*;

final class LinnEvent {
    final String service;
    final Map<String,String> values;
    LinnEvent(String service,Map<String,String> values){this.service=service;this.values=Collections.unmodifiableMap(new HashMap<>(values));}
    static LinnEvent parse(String service,byte[] body)throws Exception {
        Map<String,String> values=new HashMap<>();
        Set<String> allowed=new HashSet<>(Arrays.asList("Id","IdArray","TransportState","Shuffle","Repeat","Seconds","Duration","TrackCount","Volume","Mute","SourceIndex","SourceXmlChangeCount"));
        Element root=OpenHome.xml(body).getDocumentElement();
        if(!"propertyset".equals(root.getLocalName()) || !"urn:schemas-upnp-org:event-1-0".equals(root.getNamespaceURI()))throw new IOException("Invalid OpenHome event");
        NodeList properties=root.getElementsByTagNameNS("urn:schemas-upnp-org:event-1-0","property");
        if(properties.getLength()>32)throw new IOException("Too many event properties");
        for(int i=0;i<properties.getLength();i++){
            NodeList fields=properties.item(i).getChildNodes();
            for(int at=0;at<fields.getLength();at++)if(fields.item(at) instanceof Element){
                Element field=(Element)fields.item(at);String key=field.getLocalName(),value=field.getTextContent();
                if(!allowed.contains(key))continue;
                if("TransportState".equals(key)){
                    if(!Arrays.asList("Playing","Paused","Stopped","Buffering").contains(value))throw new IOException("Invalid transport event");
                }else if("IdArray".equals(key))OpenHome.decodeIds(value);
                else {long number=Long.parseLong(value);if(number<0 || number>0xffffffffL)throw new IOException("Invalid numeric event");}
                if(values.put(key,value)!=null)throw new IOException("Duplicate event property");
            }
        }
        return new LinnEvent(service,values);
    }
}
