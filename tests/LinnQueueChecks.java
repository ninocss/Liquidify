package dev.nino.squareglass;
import java.util.*;
public final class LinnQueueChecks {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        LinnQueuePlan.capacity(148,200,1000);
        for(int[] values:new int[][]{{0,1001,1000},{999,2,1000},{0,0,1000}}){
            boolean rejected=false;try{LinnQueuePlan.capacity(values[0],values[1],values[2]);}catch(IllegalArgumentException expected){rejected=true;}check(rejected);
        }
        List<Long> before=Arrays.asList(1L,2L),staged=Arrays.asList(3L,4L);
        check(LinnQueuePlan.expected(before,staged).equals(Arrays.asList(1L,2L,3L,4L)));
        check(before.equals(Arrays.asList(1L,2L)));
        check(LinnQueuePlan.confirmedIndex(staged,4L)==1);
        check(LinnQueuePlan.confirmedIndex(staged,99L)==-1);
        boolean rejected=false;try{LinnQueuePlan.expected(before,Arrays.asList(2L));}catch(IllegalArgumentException expected){rejected=true;}check(rejected);
        check(LinnQueuePlan.sequenceNewer(0xffffffffL,0L));
        check(!LinnQueuePlan.sequenceNewer(10,10));
        check(!LinnQueuePlan.sequenceNewer(10,9));
        check(LinnQueuePlan.sequenceNewer(10,11));
        LinnQueuePlan.stagingCapacity(1000,1000,0,1000);
        Map<String,Long> known=new HashMap<>();List<Long> original=new ArrayList<>();
        for(int i=0;i<8;i++){known.put("occ"+i,(long)i+1);original.add((long)i+1);}
        Random random=new Random(7);
        for(int sample=0;sample<100;sample++){
            List<String> desired=new ArrayList<>(known.keySet());Collections.shuffle(desired,random);
            int[] lengths=new int[desired.size()];int longest=0;
            for(int i=0;i<desired.size();i++){lengths[i]=1;for(int j=0;j<i;j++)if(known.get(desired.get(j))<known.get(desired.get(i)))lengths[i]=Math.max(lengths[i],lengths[j]+1);longest=Math.max(longest,lengths[i]);}
            Map<String,Long> retained=LinnQueuePlan.retained(desired,known,original);check(retained.size()==longest);
            long last=0;for(String key:desired)if(retained.containsKey(key)){check(retained.get(key)>last);last=retained.get(key);}
        }
    }
}
