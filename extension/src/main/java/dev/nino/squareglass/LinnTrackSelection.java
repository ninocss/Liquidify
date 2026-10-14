/* GPL-3.0-or-later. Async stream confirmation without blocking the control lane. */
package dev.nino.squareglass;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.*;

final class LinnTrackSelection implements AutoCloseable {
    private final ScheduledExecutorService worker;
    private final Supplier<OpenHome> transport;
    private final BooleanSupplier valid;
    private final Runnable completed;
    private final Consumer<Exception> failed;
    private volatile Goal goal;
    private static final class Goal {
        final long id,end;
        final boolean selectStream;
        volatile int millis;
        volatile boolean play;
        volatile boolean preserveTransport;
        int phase,appliedSeek=-1;
        Boolean appliedPlay;
        ScheduledFuture<?> next;
        Goal(long id,int millis,boolean play,boolean selectStream){this.id=id;this.millis=millis;this.play=play;this.selectStream=selectStream;preserveTransport=!selectStream;phase=selectStream?0:1;end=System.nanoTime()+TimeUnit.SECONDS.toNanos(selectStream?20:5);}
    }
    LinnTrackSelection(ScheduledExecutorService worker,Supplier<OpenHome> transport,BooleanSupplier valid,Runnable completed,Consumer<Exception> failed){
        this.worker=worker;this.transport=transport;this.valid=valid;this.completed=completed;this.failed=failed;
    }
    boolean active(){return goal!=null;}
    synchronized void select(long id,int millis,boolean play){request(id,millis,play,true);}
    synchronized void seekCurrent(long id,int millis,boolean play){request(id,millis,play,false);}
    private void request(long id,int millis,boolean play,boolean selectStream){Goal requested=new Goal(id,Math.max(0,millis),play,selectStream);goal=requested;worker.execute(() -> step(requested));}
    synchronized boolean play(boolean play){Goal current=goal;if(current==null)return false;current.preserveTransport=false;current.play=play;return true;}
    synchronized boolean seek(int millis){Goal current=goal;if(current==null)return false;current.millis=Math.max(0,millis);return true;}
    private void step(Goal current){
        if(goal!=current)return;
        if(!valid.getAsBoolean()){
            boolean cancelled=false;synchronized(this){if(goal==current){goal=null;cancelled=true;}}
            if(cancelled)completed.run();return;
        }
        if(current.next!=null)current.next.cancel(false);
        OpenHome.deadline.set(Math.min(current.end,System.nanoTime()+TimeUnit.SECONDS.toNanos(current.phase==0?3:1)));
        try{
            if(System.nanoTime()>=current.end)throw new OpenHome.Fault(OpenHome.Failure.TIMEOUT,"Track selection",0,"","Linn did not confirm the selected track",null);
            OpenHome device=transport.get();
            switch(current.phase){
                case 0:
                    device.call("Playlist","SeekId",Long.toString(current.id));current.phase=1;break;
                case 1:
                    if(Long.toString(current.id).equals(device.read("Playlist","Id")))current.phase=2;
                    else if(!current.selectStream)throw new OpenHome.Fault(OpenHome.Failure.CONFLICT,"Seek",0,"","Linn current track changed; retry seeking",null);
                    break;
                case 2: {
                    int millis=current.millis;
                    if(current.selectStream && current.appliedSeek<0 && millis==0){current.appliedSeek=0;current.phase=3;break;}
                    if(current.appliedSeek!=millis){
                        try{device.call("Playlist","SeekSecondAbsolute",Integer.toString(millis/1000));current.appliedSeek=millis;}
                        catch(OpenHome.Fault failure){
                            if(failure.kind==OpenHome.Failure.REJECTED && "803".equals(failure.code))break;
                            if(failure.kind!=OpenHome.Failure.UNCERTAIN)throw failure;
                            current.appliedSeek=millis; // Reconcile the idempotent seek before issuing another.
                        }
                    }
                    Map<String,String> time=device.call("Time","Time");
                    if(Math.abs(Long.parseLong(time.get("Seconds"))-millis/1000)<=2)current.phase=3;
                    break;
                }
                case 3: {
                    if(current.appliedSeek!=current.millis){current.phase=2;break;}
                    if(current.preserveTransport){
                        boolean confirmed=false;synchronized(this){if(goal==current && current.preserveTransport && current.appliedSeek==current.millis){goal=null;confirmed=true;}}
                        if(confirmed){completed.run();return;}
                    }
                    boolean play=current.play;
                    if(!play || !"Playing".equals(device.read("Playlist","TransportState")))device.call("Playlist",play?"Play":"Pause");
                    current.appliedPlay=play;current.phase=4;break;
                }
                case 4:
                    if(current.appliedSeek!=current.millis){current.phase=2;break;}
                    if(current.appliedPlay!=current.play){current.phase=3;break;}
                    if(Long.toString(current.id).equals(device.read("Playlist","Id")) && (current.play?"Playing":"Paused").equals(device.read("Playlist","TransportState"))){
                        boolean confirmed=false;
                        synchronized(this){if(goal==current && current.appliedPlay==current.play && current.appliedSeek==current.millis){goal=null;confirmed=true;}}
                        if(confirmed){completed.run();return;}
                    }
                    break;
                default: throw new IllegalStateException("Invalid selection phase");
            }
            if(goal==current)current.next=worker.schedule(() -> step(current),current.phase==1 || current.phase==4?100:0,TimeUnit.MILLISECONDS);
        }catch(Exception failure){if(goal==current){goal=null;failed.accept(failure);}}
        finally{OpenHome.deadline.remove();}
    }
    public synchronized void close(){Goal previous=goal;goal=null;if(previous!=null && previous.next!=null)previous.next.cancel(false);}
}
