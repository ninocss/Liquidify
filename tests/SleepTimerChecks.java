package dev.nino.squareglass;

public final class SleepTimerChecks {
    static void check(boolean value) { if(!value) throw new AssertionError(); }
    public static void main(String[] args) {
        SleepDeadline timer=new SleepDeadline();
        check(!timer.active() && timer.remaining(1000)==0);
        int first=timer.start(1000,60000);
        check(timer.active() && timer.remaining(31000)==30000);
        check(!timer.consume(first,60999));
        check(timer.consume(first,61000));
        check(!timer.active() && !timer.consume(first,62000));
        int cancelled=timer.start(100000,60000);
        timer.cancel(); check(!timer.consume(cancelled,200000));
        int old=timer.start(300000,60000);
        int replacement=timer.start(310000,120000);
        check(!timer.consume(old,500000) && timer.active());
        check(timer.remaining(370000)==60000 && !timer.consume(replacement,429999));
        check(timer.consume(replacement,500000) && !timer.consume(replacement,500001));
        for(long invalid:new long[]{0,-1,180*60000L+1}) {
            try { timer.start(1000,invalid); throw new AssertionError("Accepted invalid duration"); }
            catch(IllegalArgumentException expected) {}
        }
        int max=timer.start(2000,180*60000L);
        check(timer.consume(max,2000+180*60000L));
        TrackEndGate end=new TrackEndGate();
        check(!end.consume(1,0,60000));
        end.start(1,1000); check(!end.consume(1,2000,60000));
        check(!end.consume(1,2000,60000)); // paused playback must not expire
        check(!end.consume(1,0,60000)); // early seek back is not track completion
        check(!end.consume(1,59800,60000));
        check(end.consume(1,0,60000) && !end.consume(1,0,60000)); // repeat, one shot
        end.start(1,1000); check(end.consume(2,0,60000)); // queue advances
        end.start(1,59900); check(end.consume(1,60000,60000)); // final queue item
        end.start(1,0); end.cancel(); check(!end.consume(2,0,60000));
        end.start(1,0); check(!end.consume(1,0,0)); // unknown duration
        System.out.println("10 track end checks passed");
        System.out.println("12 sleep deadline checks passed");
    }
}
