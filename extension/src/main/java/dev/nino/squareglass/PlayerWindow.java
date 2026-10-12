/* SquareGlass. GPL-3.0-or-later. Owns a menu-scoped snapshot of the player window. */
package dev.nino.squareglass;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RenderNode;
import android.view.View;
import java.lang.ref.WeakReference;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public final class PlayerWindow {
    private static WeakReference<View> player = new WeakReference<>(null);
    private static Snapshot pending;
    private PlayerWindow() {}
    public static void attach(View decor) { player = new WeakReference<>(decor); }
    public static View source(Activity activity) {
        View view=player.get();
        return view!=null && view.isAttachedToWindow() ? view : activity.getWindow().getDecorView();
    }
    public static Function0<Unit> menuAction(Function0<Unit> action) {
        return action instanceof MenuAction ? action : new MenuAction(action);
    }
    private static final class MenuAction implements Function0<Unit> {
        private final Function0<Unit> action;
        MenuAction(Function0<Unit> action) { this.action=action; }
        public Unit invoke() {
            if (pending!=null) pending.release();
            pending=null;
            View view=player.get();
            if(view!=null && view.isAttachedToWindow() && view.getWidth()>0 && view.getHeight()>0)
                pending=new Snapshot(view);
            Snapshot captured=pending;
            if(captured!=null) new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if(pending==captured) { pending=null; captured.release(); }
            },10000);
            return action.invoke();
        }
    }
    public static Snapshot take() { Snapshot snapshot=pending; pending=null; return snapshot; }
    public static final class Snapshot {
        private final RenderNode node=new RenderNode("SquareGlass player menu source");
        public final int x,y,width,height;
        Snapshot(View source) {
            int[] location=new int[2]; source.getLocationOnScreen(location);
            x=location[0]; y=location[1]; width=source.getWidth(); height=source.getHeight();
            node.setPosition(0,0,width,height);
            Canvas canvas=node.beginRecording(width,height);
            try { source.draw(canvas); } finally { node.endRecording(); }
        }
        public View view(Context context, boolean dim) {
            return new View(context) {
                private final int[] location=new int[2];
                protected void onDraw(Canvas canvas) {
                    if(canvas.isHardwareAccelerated() && node.hasDisplayList()) {
                        ((View)this).getLocationOnScreen(location); canvas.save();
                        canvas.translate(x-location[0],y-location[1]); canvas.drawRenderNode(node); canvas.restore();
                        if(dim) canvas.drawColor(0x55000000);
                    }
                }
            };
        }
        public void release() { node.discardDisplayList(); }
    }
}
