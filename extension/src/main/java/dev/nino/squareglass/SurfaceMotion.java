/* GPL-3.0-or-later. AndroidLiquidGlass spring presentation; no action interception. */
package dev.nino.squareglass;

import android.view.View;

public final class SurfaceMotion {
    private SurfaceMotion() {}
    public static void enter(View view, boolean menu, boolean nativeSlide) {
        LiquidViewMotion.cancel(view,"enter");
        // Native sheets/queue already animate their own window. An additional
        // spring changing alpha can outlive entry and fight a quick Back exit.
        if(nativeSlide) return;
        if(!SquareSettings.motion()) { reset(view); if(!menu && !nativeSlide)view.setTranslationY(0f); return; }
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) {}
            public void onViewDetachedFromWindow(View v) {
                LiquidViewMotion.cancel(v,"enter"); reset(v);if(!menu && !nativeSlide)v.setTranslationY(0f);v.removeOnAttachStateChangeListener(this);
            }
        });
        final float travel=18*view.getResources().getDisplayMetrics().density;
        // Material owns a sheet's drag/dismiss translation. Only custom panels move.
        LiquidViewMotion.animate(view,"enter",0f,1f,false,progress -> {
            view.setAlpha(Math.max(0f,Math.min(1f,nativeSlide ? .85f+.15f*progress : progress)));
            if(menu) {
                view.setScaleX(.965f+.035f*progress);
                view.setScaleY(.98f+.02f*progress);
            }
            if(!menu && !nativeSlide)view.setTranslationY(travel*(1f-progress));
        },() -> { reset(view); if(!menu && !nativeSlide)view.setTranslationY(0f); });
    }
    public static void reset(View view) {
        view.setAlpha(1f);view.setScaleX(1f);view.setScaleY(1f);
        // Do not overwrite native BottomSheetBehavior translation.
    }
}
