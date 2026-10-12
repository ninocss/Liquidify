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
    public static List<Long> expected(List<Long> before,List<Long> inserted){
        List<Long> result=new ArrayList<>(before);result.addAll(inserted);
        if(new HashSet<>(result).size()!=result.size())throw new IllegalArgumentException("Duplicate OpenHome queue ID");
        return result;
    }
    public static boolean sequenceNewer(long previous,long next){
        long delta=(next-previous)&0xffffffffL;return delta!=0 && delta<0x80000000L;
    }
}
