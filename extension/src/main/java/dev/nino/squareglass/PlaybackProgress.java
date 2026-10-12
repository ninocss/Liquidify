/* GPL-3.0-or-later. Native times use milliseconds. */
package dev.nino.squareglass;
public final class PlaybackProgress {
    private PlaybackProgress() {}
    public static float fraction(int position, int duration) {
        if(duration<=0)return -1f;
        return Math.max(0f,Math.min(1f,(float)position/duration));
    }
}
