/* GPL-3.0-or-later. Reversible queue delta, guarded against concurrent controllers. */
package dev.nino.squareglass;

import java.io.IOException;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

final class LinnQueueTransaction {
    static final class Track {
        final String key,uri,metadata;
        Track(String key,String uri,String metadata){this.key=key;this.uri=uri;this.metadata=metadata;}
    }
    final OpenHome device;
    final List<Long> original,expected,remote=new ArrayList<>(),inserted=new ArrayList<>();
    final Map<Long,Map<String,String>> backup;
    final List<Track> desired;
    final Map<String,Long> retained;
    private boolean uncertain;
    LinnQueueTransaction(OpenHome device,List<Track> desired,List<String> knownKeys,List<Long> knownIds,int max)throws Exception {
        this.device=device;this.desired=desired;original=device.ids();expected=new ArrayList<>(original);
        backup=device.readTracks(original);Map<String,Long> reusable=new HashMap<>();
        if(original.equals(knownIds) && knownKeys.size()==knownIds.size()) {
            Map<String,Long> known=new HashMap<>();for(int i=0;i<knownKeys.size();i++)known.put(knownKeys.get(i),knownIds.get(i));
            for(Track track:desired){Long id=known.get(track.key);Map<String,String> entry=backup.get(id);
                if(entry!=null && track.uri.equals(entry.get("Uri")) && track.metadata.equals(entry.get("Metadata")))reusable.put(track.key,id);}
        }
        List<String> keys=new ArrayList<>();for(Track track:desired)keys.add(track.key);
        retained=LinnQueuePlan.retained(keys,reusable,original);
        LinnQueuePlan.stagingCapacity(original.size(),desired.size(),desired.size()-retained.size(),max);
    }
    void stage(BooleanSupplier current,IntConsumer progress)throws Exception {
        assertExpected();long after=0;
        for(Track track:desired) {
            requireCurrent(current);
            Long id=retained.get(track.key);
            if(id==null) {
                try{id=device.insertConfirmed(expected,after,track.uri,track.metadata);}
                catch(OpenHome.Fault failure){if(failure.kind==OpenHome.Failure.UNCERTAIN)uncertain=true;throw failure;}
                expected.add(after==0?0:expected.indexOf(after)+1,id);inserted.add(id);
            }
            remote.add(id);after=id;requireCurrent(current);progress.accept(remote.size());
        }
        assertExpected();
    }
    void commit(BooleanSupplier current)throws Exception {
        Set<Long> keep=new HashSet<>(remote);
        for(long id:original)if(!keep.contains(id)) {
            requireCurrent(current);assertExpected();
            try{device.call("Playlist","DeleteId",Long.toString(id));}
            catch(OpenHome.Fault failure){if(failure.kind==OpenHome.Failure.UNCERTAIN)uncertain=true;throw failure;}
            expected.remove(id);
        }
        assertExpected();
        if(!expected.equals(remote))throw new IOException("Linn queue order was not confirmed");
    }
    Map<Long,Long> rollback()throws Exception {
        verifyRollback();
        for(long id:inserted) {
            assertExpected();device.call("Playlist","DeleteId",Long.toString(id));expected.remove(id);
        }
        Map<Long,Long> restored=new HashMap<>();long after=0;
        for(long id:original) {
            assertExpected();long replacement=id;
            if(!expected.contains(id)) {
                Map<String,String> entry=backup.get(id);
                replacement=device.insertConfirmed(expected,after,entry.get("Uri"),entry.get("Metadata"));
                expected.add(after==0?0:expected.indexOf(after)+1,replacement);
            }
            restored.put(id,replacement);after=replacement;
        }
        assertExpected();return restored;
    }
    void verifyRollback()throws Exception {
        if(uncertain)throw new IOException("Recovery paused: device write outcome is uncertain");
        assertExpected();
    }
    private void assertExpected()throws Exception {
        if(!device.ids().equals(expected))throw new OpenHome.Fault(OpenHome.Failure.CONFLICT,"Queue transaction",0,"","Linn queue changed in another controller",null);
    }
    static void requireCurrent(BooleanSupplier current)throws IOException {
        if(!current.getAsBoolean())throw new IOException("Linn handoff was cancelled");
    }
}
