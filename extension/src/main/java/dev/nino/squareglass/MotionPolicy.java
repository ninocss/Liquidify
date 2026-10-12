/* GPL-3.0-or-later. Shared quiet-time policy for decorative icon motion. */
package dev.nino.squareglass;
public final class MotionPolicy {
    private long interaction, lastFire=-12000;
    private boolean touching;
    public MotionPolicy(long now) {interaction=now;}
    public void touch(long now,boolean down) {interaction=now;touching=down;}
    public boolean ready(long now,boolean allowed) {
        return allowed && !touching && now-interaction>=5000 && now-lastFire>=12000;
    }
    public void fired(long now) {lastFire=now;}
    public static boolean visible(float left,float top,float right,float bottom,float viewLeft,float viewTop,float width,float height) {
        return right>left && bottom>top && width>0 && height>0 &&
            right>viewLeft && left<viewLeft+width && bottom>viewTop && top<viewTop+height;
    }
}
