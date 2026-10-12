/* GPL-3.0-or-later. One-shot presentation motion; no native action interception. */
package dev.nino.squareglass;

import android.view.View;
import android.view.animation.PathInterpolator;

public final class SurfaceMotion {
    private SurfaceMotion() {}
    public static void enter(View view, boolean menu, boolean nativeSlide) {
        view.animate().cancel();
        if(!SquareSettings.motion()) { reset(view); return; }
        view.setAlpha(nativeSlide ? .85f : 0f);
        if(menu) { view.setScaleX(.965f); view.setScaleY(.965f); }
        // Material already owns its sheet drag/dismiss translation.
        else if(!nativeSlide)view.setTranslationY(18*view.getResources().getDisplayMetrics().density);
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) {}
            public void onViewDetachedFromWindow(View v) {
                v.animate().cancel(); reset(v);v.removeOnAttachStateChangeListener(this);
            }
        });
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
            .setDuration(menu?220:260).setInterpolator(new PathInterpolator(.2f,0f,0f,1f)).start();
    }
    public static void reset(View view) {
        view.setAlpha(1f);view.setScaleX(1f);view.setScaleY(1f);view.setTranslationY(0f);
    }
}
