/* GPL-3.0-or-later. Native transitions own drawing until two stable traversals. */
package dev.nino.squareglass;

public final class BackdropCapturePolicy {
    private int stableFrames = 2;

    public void transition() { stableFrames = 2; }

    public boolean allowCapture(boolean transitioning) {
        if (transitioning) {
            transition();
            return false;
        }
        if (stableFrames > 0) {
            stableFrames--;
            return false;
        }
        return true;
    }
}
