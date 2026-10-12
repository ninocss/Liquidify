/* GPL-3.0-or-later. Monotonic, replaceable, one-shot timer state. */

package dev.nino.squareglass;

public final class SleepDeadline {

    private long deadline;

    private int revision;

    public int start(long now, long duration) {

        if(duration<=0 || duration>180*60000L) throw new IllegalArgumentException("Duration must be 1 to 180 minutes");

        deadline=Math.addExact(now,duration); return ++revision;

    }

    public void cancel() { deadline=0; revision++; }

    public long remaining(long now) { return deadline==0 ? 0 : Math.max(0,deadline-now); }

    public long deadline() { return deadline; }

    public boolean active() { return deadline!=0; }

    public boolean consume(int token,long now) {

        if(token!=revision || !active() || now<deadline) return false;

        cancel(); return true;

    }

}

