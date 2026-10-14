package dev.nino.squareglass;

import java.util.*;

/** Device IDs identify queue occurrences, so duplicate TIDAL titles remain distinct. */
public final class LinnQueuePlan {
    private LinnQueuePlan(){}
    public static void capacity(int oldSize,int incoming,int max) {
        if(incoming<1 || incoming>max)throw new IllegalArgumentException("Queue exceeds the Linn capacity of "+max+" tracks.");
        if((long)oldSize+incoming>max)throw new IllegalArgumentException("Not enough free Linn queue space for a reversible transfer. Clear its old queue in Linn first.");
    }
    public static int confirmedIndex(List<Long> ids,long id){return ids.indexOf(id);}
    public static void stagingCapacity(int oldSize,int finalSize,int inserted,int max) {
        if(finalSize<1 || finalSize>max || inserted<0 || (long)oldSize+inserted>max)
            throw new IllegalArgumentException("Not enough Linn queue space for a reversible transfer.");
    }
    /** Keep the longest ordered set of unique occurrences; a title may occur repeatedly. */
    public static Map<String,Long> retained(List<String> desired,Map<String,Long> known,List<Long> original) {
        Map<Long,Integer> positions=new HashMap<>();for(int i=0;i<original.size();i++)positions.put(original.get(i),i);
        int size=desired.size(),length=0;int[] tails=new int[size],prior=new int[size],values=new int[size];
        Arrays.fill(prior,-1);Arrays.fill(values,-1);Set<String> unique=new HashSet<>();
        for(int i=0;i<size;i++) {
            String key=desired.get(i);if(!unique.add(key))throw new IllegalArgumentException("Duplicate native queue occurrence");
            Integer position=positions.get(known.get(key));if(position==null)continue;values[i]=position;
            int lo=0,hi=length;while(lo<hi){int mid=(lo+hi)>>>1;if(values[tails[mid]]<position)lo=mid+1;else hi=mid;}
            if(lo>0)prior[i]=tails[lo-1];tails[lo]=i;if(lo==length)length++;
        }
        Map<String,Long> retained=new LinkedHashMap<>();
        if(length>0)for(int i=tails[length-1];i>=0;i=prior[i])retained.put(desired.get(i),known.get(desired.get(i)));
        return retained;
    }
    public static List<Long> expected(List<Long> before,List<Long> inserted){
        List<Long> result=new ArrayList<>(before);result.addAll(inserted);
        if(new HashSet<>(result).size()!=result.size())throw new IllegalArgumentException("Duplicate OpenHome queue ID");
        return result;
    }
    public static boolean sequenceNewer(long previous,long next){
        long delta=(next-previous)&0xffffffffL;return delta!=0 && delta<0x80000000L;
    }
}
