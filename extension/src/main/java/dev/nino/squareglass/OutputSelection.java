/* GPL-3.0-or-later. Confirm selection only from authoritative device state. */
package dev.nino.squareglass;

public final class OutputSelection {
    private String target;
    private long expires;
    private int revision;
    public int nextRequest() { return ++revision; }
    public boolean accepts(int value) { return value == revision; }
    public void invalidate() { ++revision; }
    public void begin(String id, long now) { target=id; expires=now+15000; }
    public String target() { return target; }
    /** 1 confirmed, -1 timed out, 0 pending/idle. */
    public int observe(String active, long now) {
        if(target==null) return 0;
        if(target.equals(active)) {target=null;return 1;}
        if(now>=expires) {target=null;return -1;}
        return 0;
    }
    public void failed() {target=null;}
}
