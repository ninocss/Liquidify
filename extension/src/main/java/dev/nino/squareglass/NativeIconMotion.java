/* GPL-3.0-or-later. Stable native hook ABI for library glass feedback. */
package dev.nino.squareglass;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

public final class NativeIconMotion {
    private NativeIconMotion() {}
    public static void attach(View view) { NativeIconGlass.attach(view); }
    public static void detach(View view) { NativeIconGlass.detach(view); }
    public static void touch(View view,MotionEvent event) { NativeIconGlass.touch(view,event); }
    public static int before(View parent,View child,Canvas canvas) {
        return NativeIconGlass.before(parent,child,canvas);
    }
    public static void after(View parent,View child,Canvas canvas,int save) {
        if(save >= 0) canvas.restoreToCount(save);
    }
}
