/* GPL-3.0-or-later. Scroll-derived state never retains a previous opaque header. */
package dev.nino.squareglass;
public final class HeaderProgress {
    private HeaderProgress() {}
    public static float opacity(float offset,float reach) {
        if(!Float.isFinite(offset) || offset<=0 || reach<=0) return 0;
        float t=Math.min(1,offset/reach);
        // Establish the scrim early as content moves behind the controls. Keep
        // its existing reach and drawable bounds; scrolling back still clears it.
        float remaining=1-t;
        return 1-remaining*remaining*remaining;
    }
}
