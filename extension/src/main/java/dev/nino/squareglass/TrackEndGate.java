/* GPL-3.0-or-later. One-shot current-track completion, including repeat. */
package dev.nino.squareglass;
public final class TrackEndGate {
    private boolean active;
    private int id,previous;
    public void start(int id,int position) { this.id=id; previous=position; active=true; }
    public void cancel() { active=false; }
    public boolean active() { return active; }
    public boolean consume(int currentId,int position,int duration) {
        if(!active) return false;
        boolean ended=currentId!=id || (duration>0 && position>=duration) ||
                (duration>0 && previous>=duration-500 && position<previous-500);
        previous=position;
        if(ended) { active=false; return true; }
        return false;
    }
}
