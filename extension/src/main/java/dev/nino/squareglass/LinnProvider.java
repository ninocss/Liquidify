/* SquareGlass. GPL-3.0-or-later. */
package dev.nino.squareglass;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.aspiro.wamp.broadcast.*;
import com.aspiro.wamp.player.k0;
import java.util.*;
import java.util.concurrent.*;
import io.reactivex.Single;

public final class LinnProvider implements o,q {
    static LinnProvider instance;
    final Application app;
    final Handler main=new Handler(Looper.getMainLooper());
    final ScheduledExecutorService io=Executors.newSingleThreadScheduledExecutor();
    final ScheduledExecutorService discovery=Executors.newSingleThreadScheduledExecutor();
    final CopyOnWriteArrayList<l> listeners=new CopyOnWriteArrayList<>();
    final ConcurrentHashMap<String,Device> devices=new ConcurrentHashMap<>();
    final LinnNativeVolume nativeVolume=new LinnNativeVolume(this);
    volatile Device connected;
    volatile LinnPlayer player;
    volatile boolean connecting,scanning;
    private volatile int scanRevision;
    private ScheduledFuture<?> scan;
    private android.net.wifi.WifiManager.MulticastLock multicast;
    String error="";
    static final class Device extends p2.a {
        final OpenHome transport;
        Device(OpenHome transport){super("LINN_"+transport.uuid,transport.name,0);this.transport=transport;}
        @Override public boolean a(){return true;}
    }
    public LinnProvider(Application app){this.app=app;instance=this;}
    public o get(){return this;}
    public boolean isAvailable(){return true;}
    public void refreshAvailability(){}
    public void addListener(q.a listener){}
    public void addListener(l listener){listeners.addIfAbsent(listener);}
    public com.aspiro.wamp.broadcast.p getBroadcastProviderButton(){return null;}
    public com.aspiro.wamp.broadcast.r getBroadcastProviderGroupButton(){return null;}
    public fq.f getVolumeControl(){return nativeVolume;}
    public p2.a getConnectedItem(){return connected;}
    public boolean isConnected(){return connected!=null;}
    public boolean isConnecting(){return connecting;}
    public boolean isValidItem(p2.a item){return item instanceof Device;}
    public void init(){main.postDelayed(() -> resume(0),2500);}
    void resume(int attempt){
        if(connected!=null || connecting || attempt>=5)return;
        android.content.SharedPreferences prefs=app.getSharedPreferences("squareglass_linn",0);
        String location=prefs.getString("location","");
        if(location.isEmpty() || prefs.getString("sessionUuid","").isEmpty())return;
        k0 local;
        try{local=NativeLinnBridge.player();}catch(RuntimeException failure){main.postDelayed(() -> resume(attempt+1),2000);return;}
        if(local==null || !local.isLocal()){main.postDelayed(() -> resume(attempt+1),2000);return;}
        io.execute(() -> {
            try {
                OpenHome transport;
                try{transport=new OpenHome(location);}catch(Exception moved){
                    transport=null;
                    for(String candidate:OpenHome.discover(3500))try{
                        OpenHome found=new OpenHome(candidate);
                        if(found.uuid.equals(prefs.getString("sessionUuid",""))){transport=found;break;}
                    }catch(Exception ignored){}
                    if(transport==null)throw moved;
                }
                Device device=new Device(transport);LinnPlayer restored=new LinnPlayer(this,device,local);restored.reattach();
                main.post(() -> {
                    if(connected!=null || connecting)return;
                    local.onActionPause();restored.restoreNativeQueue();
                    devices.put(device.a,device);connected=device;player=restored;
                    NativeLinnBridge.activate(restored);for(l listener:listeners)listener.a(this);restored.start();
                });
            }catch(Exception failure){fail("Could not reattach Linn. Its device queue was left unchanged.");}
        });
    }
    public Single<List<p2.a>> getAllAvailableDevices(){return Single.fromCallable(() -> new ArrayList<p2.a>(devices.values()));}
    public void startScanning(){
        if(multicast==null)try {
            android.net.wifi.WifiManager wifi=(android.net.wifi.WifiManager)app.getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
            if(wifi!=null){multicast=wifi.createMulticastLock("SquareGlass Linn discovery");multicast.setReferenceCounted(false);multicast.acquire();}
        }catch(RuntimeException unavailable){multicast=null;}
        scanning=true;int revision=++scanRevision;
        if(scan!=null)scan.cancel(false);
        scan=discovery.schedule(() -> discover(revision),0,TimeUnit.MILLISECONDS);
    }
    void discover(int revision){
        try {
            Set<String> locations=OpenHome.discover(3500);
            String cached=app.getSharedPreferences("squareglass_linn",0).getString("location","");
            if(!cached.isEmpty())locations.add(cached);
            for(String location:locations) {
                if(!scanning || revision!=scanRevision)return;
                try {
                    OpenHome transport=new OpenHome(location);Device device=new Device(transport);
                    devices.put(device.a,device);
                    if(connected!=null && connected.a.equals(device.a)){
                        connected=device;LinnPlayer active=player;if(active!=null && (active.closed || !active.device.transport.location.equals(location)))io.execute(() -> active.readdress(device));
                    }
                } catch(Exception ignored) { /* Non-Linn, vanished or incomplete renderer. */ }
            }
            main.post(() -> {if(scanning && revision==scanRevision)for(l listener:listeners)listener.c();});
        } catch(Exception failure){fail("Linn discovery failed. Check the local network and retry.");}
        if(scanning && revision==scanRevision)scan=discovery.schedule(() -> discover(revision),8,TimeUnit.SECONDS);
    }
    public void stopScanning(){scanning=false;++scanRevision;if(scan!=null)scan.cancel(false);if(multicast!=null){if(multicast.isHeld())multicast.release();multicast=null;}}
    public static void addAddress(String address){
        LinnProvider p=instance;if(p==null)return;
        if(!address.startsWith("http://") && !address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")){p.fail("Enter a local IP or the discovered device description URL.");return;}
        p.discovery.execute(() -> {
            try {
                boolean found=false;
                Set<String> locations=address.startsWith("http://")?Collections.singleton(address):OpenHome.discover(address,3500);
                for(String location:locations) {
                    OpenHome transport=new OpenHome(location);Device device=new Device(transport);p.devices.put(device.a,device);found=true;
                    p.app.getSharedPreferences("squareglass_linn",0).edit().putString("location",location).apply();
                }
                if(!found)throw new Exception("No Linn renderer answered at this address.");
                p.main.post(() -> {for(l listener:p.listeners)listener.c();SquareOutput.changed();});
            }catch(Exception failure){p.fail(failure.getMessage());}
        });
    }
    public void connect(p2.a item){
        android.util.Log.i("SquareLinn","Native provider connect requested");
        if(!(item instanceof Device) || connecting)return;
        Device device=(Device)item;k0 previous=NativeLinnBridge.player();
        if(previous==null || !previous.isLocal()){fail("Switch to This phone before connecting Linn.");return;}
        LinnPlayer candidate=new LinnPlayer(this,device,previous);
        try {candidate.capture();}catch(Exception failure){fail(failure.getMessage());return;}
        connecting=true;error="";
        io.execute(() -> {
            android.util.Log.i("SquareLinn","Queue transfer started");
            try {
                candidate.transfer();
                main.post(() -> {
                    player=candidate;connected=device;connecting=false;
                    NativeLinnBridge.activate(candidate);
                    app.getSharedPreferences("squareglass_linn",0).edit().putString("location",device.transport.location)
                        .putString("uuid",device.transport.uuid).apply();
                    for(l listener:listeners)listener.a(this);
                    candidate.start();
                });
            } catch(Exception | LinkageError failure){android.util.Log.e("SquareLinn","Linn adapter connection failed: "+failure.getClass().getSimpleName());main.post(() -> {
                connecting=false;fail(failure.getMessage());
                if(previousOutput!=null)NativeOutputBridge.select(previousOutput);
            });}
        });
    }
    public void disconnect(o.a callback){
        LinnPlayer active=player;
        if(active==null){NativeLinnBridge.disconnected(callback);return;}
        io.execute(() -> {
            try {
                active.device.transport.call("Playlist","Pause");
                active.state=com.aspiro.wamp.enums.MusicServiceState.PAUSED;
                main.post(() -> {
                    connected=null;player=null;active.close();
                    app.getSharedPreferences("squareglass_linn",0).edit().remove("sessionUuid").remove("queue").remove("mapping").apply();
                    NativeLinnBridge.publish(com.aspiro.wamp.enums.MusicServiceState.PAUSED,active.position,active.duration);
                    NativeLinnBridge.activate(active.previous);
                    // Return paused. No unsolicited local audio on network loss or handback.
                    active.previous.onActionPause();active.previous.onActionPlayPosition(active.index,false,false);active.previous.onActionSeekTo(active.position);
                    for(l listener:listeners)listener.d(this);
                    NativeLinnBridge.disconnected(callback);
                });
            }catch(Exception failure){fail("Could not confirm Linn pause. Retry before switching outputs.");}
        });
    }
    void fail(String message){
        error=message==null?"Linn request failed. Retry.":message;
        android.util.Log.w("SquareLinn",error);
        main.post(() -> {Toast.makeText(app,error,Toast.LENGTH_LONG).show();SquareOutput.linnFailure(error);});
    }
    public static Object override(Object proposed){
        LinnProvider provider=instance;
        return provider!=null && provider.connected!=null && provider.player!=null?provider.player:proposed;
    }
    public static String label(Object device){return device instanceof Device?"Linn Majik DSM · OpenHome":null;}
    static Object previousOutput;
    public static void prepareSelection(Object item){if(item instanceof Device){android.util.Log.i("SquareLinn","Linn output selected");previousOutput=NativeOutputBridge.active();}}
    public static Object confirmed(Object nativeActive){
        LinnProvider p=instance;
        return nativeActive instanceof Device && (p==null || p.connected==null)?previousOutput:nativeActive;
    }
}
