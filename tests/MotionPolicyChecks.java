package dev.nino.squareglass;
public final class MotionPolicyChecks {
    private static void check(boolean ok) {if(!ok)throw new AssertionError();}
    public static void main(String[] args) {
        MotionPolicy p=new MotionPolicy(1000);
        check(!p.ready(5999,true));check(p.ready(6000,true));
        p.fired(6000);check(!p.ready(17999,true));check(p.ready(18000,true));
        check(!p.ready(18000,false));
        p.touch(18000,true);check(!p.ready(30000,true));
        p.touch(30000,false);check(!p.ready(34999,true));check(p.ready(35000,true));
        p.fired(35000);p.touch(47000,false);check(!p.ready(47000,true));check(p.ready(52000,true));
        // The native navbar's ComposeView occupies only the bottom 495px, at y=1929.
        check(MotionPolicy.visible(100,2200,160,2260,0,1929,1080,495));
        check(!MotionPolicy.visible(100,1800,160,1860,0,1929,1080,495));
        check(!MotionPolicy.visible(1200,2200,1260,2260,0,1929,1080,495));
        check(!MotionPolicy.visible(100,2500,160,2560,0,1929,1080,495));
        check(!MotionPolicy.visible(100,2200,100,2260,0,1929,1080,495));
        System.out.println("Motion quiet time, cadence, hold, cancellation and eligibility verified");
    }
}
