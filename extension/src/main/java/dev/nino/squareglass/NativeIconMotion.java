/* GPL-3.0-or-later. Draw-only effects for the native legacy favorite action. */
package dev.nino.squareglass;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import java.util.WeakHashMap;

public final class NativeIconMotion {
    private static final WeakHashMap<View, State> states=new WeakHashMap<>();
    private static final class State {
        final java.lang.ref.WeakReference<View> view;
        Object token; ValueAnimator animator;
        float progress=1, x,y; int kind; boolean selected,moved;
        long tap=-20000;
        State(View v) {view=new java.lang.ref.WeakReference<>(v);selected=v.isSelected();}
        void fire(int next) {
            View v=view.get();if(v==null || !SquareSettings.motion())return;
            if(animator!=null)animator.cancel();kind=next;
            animator=ValueAnimator.ofFloat(0,1);animator.setDuration(next==2?620:460);
            animator.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();View target=view.get();if(target!=null)target.invalidate();});
            animator.start();
        }
    }
    public static void attach(View v) {
        if(v.getId()!=0x7f0b02c6 || states.containsKey(v))return;
        State s=new State(v);states.put(v,s);s.token=IconMotion.registerNative(v,s::fire);
    }
    public static void detach(View v) {
        State s=states.remove(v);if(s==null)return;
        IconMotion.unregisterNative(s.token);if(s.animator!=null)s.animator.cancel();v.invalidate();
    }
    public static void touch(View v,MotionEvent event) {
        State s=states.get(v);if(s==null)return;
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN) {s.x=event.getX();s.y=event.getY();s.moved=false;s.fire(1);}
        if(action==MotionEvent.ACTION_MOVE && Math.hypot(event.getX()-s.x,event.getY()-s.y)>ViewConfiguration.get(v.getContext()).getScaledTouchSlop())s.moved=true;
        if(action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_POINTER_DOWN)s.moved=true;
        if(action==MotionEvent.ACTION_UP && !s.moved)s.tap=SystemClock.uptimeMillis();
    }
    public static int before(View parent,View child,Canvas canvas) {
        State s=states.get(parent);if(s==null || !(child instanceof ImageView))return -1;
        boolean selected=parent.isSelected();
        if(selected!=s.selected) {
            s.selected=selected;
            if(SystemClock.uptimeMillis()-s.tap<10000) {s.fire(selected?2:3);if(selected && SquareSettings.motion())ProbeDiagnostics.likeAnimations++;}
            s.tap=-20000;
        }
        if(!SquareSettings.motion() || s.progress>=1)return -1;
        float t=s.progress,wave=(float)Math.sin(Math.PI*t),scale=s.kind==3?1-.12f*wave:
            s.kind==5?1+.12f*Math.abs((float)Math.sin(t*Math.PI*2))*(1-t):1+.15f*wave*(1-t);
        float angle=s.kind==2 || s.kind==3 || s.kind==5?0:(float)Math.sin(t*Math.PI*(s.kind==4?2:4))*(1-t)*(s.kind==4?10:7);
        int save=canvas.save();float x=child.getLeft()+child.getWidth()*.5f,y=child.getTop()+child.getHeight()*.5f;
        canvas.translate(0,-wave*2.5f*parent.getResources().getDisplayMetrics().density);
        canvas.rotate(angle,x,y);canvas.scale(scale,scale,x,y);return save;
    }
    public static void after(View parent,View child,Canvas canvas,int save) {
        if(save<0)return;canvas.restoreToCount(save);
        State s=states.get(parent);if(s==null || s.kind!=2 || !SquareSettings.motion())return;
        float t=s.progress,d=parent.getResources().getDisplayMetrics().density;
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffff6899);paint.setAlpha(Math.round(255*(1-t)));
        float cx=child.getLeft()+child.getWidth()*.5f,cy=child.getTop()+child.getHeight()*.5f;
        for(int i=0;i<6;i++) {
            double a=(i*60-90)*Math.PI/180;float radius=(12+24*t)*d,r=3*d;
            float x=cx+(float)Math.cos(a)*radius,y=cy+(float)Math.sin(a)*radius-8*d*t;
            Path p=new Path();p.moveTo(x,y+r);p.cubicTo(x-r*2,y-r*.3f,x-r,y-r*2,x,y-r*.7f);
            p.cubicTo(x+r,y-r*2,x+r*2,y-r*.3f,x,y+r);p.close();canvas.drawPath(p,paint);
        }
    }
}
