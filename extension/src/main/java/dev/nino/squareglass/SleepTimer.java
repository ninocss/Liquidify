/* GPL-3.0-or-later. One timer, one sheet, native pause. */
package dev.nino.squareglass;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.Locale;

public final class SleepTimer {
    private static final SleepDeadline timer=new SleepDeadline();
    private static final TrackEndGate trackEnd=new TrackEndGate();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static AlarmManager manager;
    private static AlarmManager.OnAlarmListener alarm;
    private static Runnable fallback,endPoll;
    private static bi.e player;
    private static WeakReference<Dialog> visible=new WeakReference<>(null);
    private SleepTimer() {}
    public static String status() {
        if(trackEnd.active()) return "Sleep timer · End of track";
        if(!timer.active()) return "Sleep timer";
        long seconds=(timer.remaining(SystemClock.elapsedRealtime())+999)/1000;
        return String.format(Locale.ENGLISH,"Sleep timer · %d:%02d",seconds/60,seconds%60);
    }
    public static void cancel() {
        if(manager!=null && alarm!=null) manager.cancel(alarm);
        if(fallback!=null) main.removeCallbacks(fallback);
        if(endPoll!=null) main.removeCallbacks(endPoll);
        alarm=null; fallback=null; endPoll=null; player=null; timer.cancel(); trackEnd.cancel();
    }
    private static void pause() {
        bi.e target=player; cancel();
        if(target!=null) try { target.d(); android.util.Log.i("SquareGlass","Sleep timer paused native playback"); }
        catch(RuntimeException error) { android.util.Log.w("SquareGlass","Sleep timer could not pause playback",error); }
    }
    public static void playerChanged(bi.e current) {
        if(!timer.active() && !trackEnd.active()) return;
        main.post(() -> {
            if(!timer.active() && !trackEnd.active()) return;
            player=current;
            if(endPoll!=null) { main.removeCallbacks(endPoll); main.post(endPoll); }
        });
    }
    public static boolean start(Context context,int minutes) {
        bi.e nativePlayer=NativePlayback.host();
        if(nativePlayer==null) { unavailable(context); return false; }
        cancel(); player=nativePlayer;
        int token=timer.start(SystemClock.elapsedRealtime(),minutes*60000L);
        Runnable finish=() -> { if(timer.consume(token,SystemClock.elapsedRealtime())) pause(); };
        manager=(AlarmManager)context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarm=finish::run;
        if(manager!=null) try { manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,timer.deadline(),"SquareGlass sleep timer",alarm,main); }
        catch(SecurityException unavailable) { android.util.Log.d("SquareGlass","Using in-process sleep timer",unavailable); }
        fallback=finish; main.postDelayed(finish,minutes*60000L); return true;
    }
    public static boolean endOfTrack(Context context) {
        bi.e current=NativePlayback.host();
        if(current==null || current.getCurrentItem()==null) { unavailable(context); return false; }
        cancel(); player=current;
        trackEnd.start(current.getCurrentItem().getMediaItem().getId(),current.getCurrentMediaPosition());
        endPoll=new Runnable() { public void run() {
            if(!trackEnd.active() || player==null) return;
            try {
                int id=player.getCurrentItem()==null ? -1 : player.getCurrentItem().getMediaItem().getId();
                int position=player.getCurrentMediaPosition(),duration=player.getCurrentMediaDuration();
                if(trackEnd.consume(id,position,duration)) { pause(); return; }
                main.postDelayed(this,duration>0 && duration-position<1000 ? 40 : 250);
            } catch(RuntimeException changingQueue) { if(trackEnd.active()) main.postDelayed(this,250); }
        }};
        main.post(endPoll); return true;
    }
    private static void unavailable(Context context) { Toast.makeText(context,"Open a track before setting a sleep timer",Toast.LENGTH_SHORT).show(); }
    private static Activity activity(Context context) {
        while(context instanceof ContextWrapper) {
            if(context instanceof Activity) return (Activity)context;
            Context next=((ContextWrapper)context).getBaseContext(); if(next==context) break; context=next;
        }
        return null;
    }
    private static int dp(Context context,int value) { return Math.round(value*context.getResources().getDisplayMetrics().density); }
    private static TextView text(Context context,String title,int size) {
        TextView label=new TextView(context); label.setText(title); label.setTextSize(size);
        label.setTextColor(SquarePalette.ink(SquarePalette.isDark()));
        label.setTypeface(context.getResources().getFont(0x7f090005)); return label;
    }
    private static TextView row(Context context,String title,Runnable action) {
        TextView view=text(context,title,17); view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(context,24),dp(context,16),dp(context,24),dp(context,16)); view.setMinimumHeight(dp(context,56));
        GradientDrawable mask=new GradientDrawable(); mask.setColor(Color.WHITE); mask.setCornerRadius(dp(context,16));
        int ink=SquarePalette.ink(SquarePalette.isDark());
        view.setBackground(new RippleDrawable(ColorStateList.valueOf((ink&0xffffff)|0x18000000),null,mask));
        view.setFocusable(true); view.setOnClickListener(v -> action.run()); return view;
    }
    public static View button(Context context,LiquidMenuSurface glass) {
        TextView label=row(context,status()+"  ›",()->{});
        label.setOnClickListener(v -> { if(!SquareMenus.sleepTimer(label)) show(context); });
        Runnable refresh=new Runnable() { public void run() { label.setText(status()+"  ›"); if(label.isAttachedToWindow()) label.postDelayed(this,1000); } };
        label.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) { refresh.run(); }
            public void onViewDetachedFromWindow(View view) { label.removeCallbacks(refresh); }
        }); return label;
    }
    /** Replaces a menu's contents; never opens another menu over it. */
    public static View content(Context context,LiquidMenuSurface glass,Runnable close) {
        GlassFrame panel=new GlassFrame(context); glass.paint(panel,30,false);
        LinearLayout page=new LinearLayout(context); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0,dp(context,8),0,dp(context,24));
        FrameLayout heading=new FrameLayout(context);
        TextView title=text(context,"Stop audio in",19); title.setGravity(Gravity.CENTER);
        heading.addView(title,new FrameLayout.LayoutParams(-1,dp(context,64)));
        TextView back=text(context,"‹",30); back.setGravity(Gravity.CENTER); back.setContentDescription("Close sleep timer");
        back.setOnClickListener(v -> close.run()); back.setFocusable(true);
        heading.addView(back,new FrameLayout.LayoutParams(dp(context,56),dp(context,64),Gravity.LEFT)); page.addView(heading);
        TextView status=text(context,"",14); status.setGravity(Gravity.CENTER); page.addView(status);
        for(int minutes:new int[]{5,10,15,30,45,60}) {
            page.addView(row(context,minutes==60?"1 hour":minutes+" minutes",()-> { if(start(context,minutes)) close.run(); }));
        }
        page.addView(row(context,"End of track",()-> { if(endOfTrack(context)) close.run(); }));
        LinearLayout custom=new LinearLayout(context); custom.setGravity(Gravity.CENTER_VERTICAL);
        NumberPicker minutes=new NumberPicker(context); minutes.setMinValue(1); minutes.setMaxValue(180); minutes.setValue(30);
        minutes.setTextColor(SquarePalette.ink(SquarePalette.isDark())); minutes.setContentDescription("Custom duration in minutes");
        custom.addView(minutes,new LinearLayout.LayoutParams(0,dp(context,120),1));
        custom.addView(row(context,"Set timer",()-> { minutes.clearFocus(); if(start(context,minutes.getValue())) close.run(); }));
        custom.setVisibility(View.GONE);
        page.addView(row(context,"Custom duration…",()->custom.setVisibility(custom.getVisibility()==View.GONE?View.VISIBLE:View.GONE)));
        page.addView(custom);
        TextView stop=row(context,"Turn off timer",()-> { cancel(); close.run(); }); page.addView(stop);
        ScrollView scroll=new ScrollView(context) {
            protected void onMeasure(int width,int height) {
                int cap=Math.round(context.getResources().getDisplayMetrics().heightPixels*.82f);
                if(View.MeasureSpec.getMode(height)!=View.MeasureSpec.UNSPECIFIED) cap=Math.min(cap,View.MeasureSpec.getSize(height));
                super.onMeasure(width,View.MeasureSpec.makeMeasureSpec(cap,View.MeasureSpec.AT_MOST));
            }
        };
        scroll.addView(page,new ScrollView.LayoutParams(-1,-2)); ((android.view.ViewGroup)panel).addView(scroll,new FrameLayout.LayoutParams(-1,-2));
        ((View)panel).setOnApplyWindowInsetsListener((view,insets)-> {
            int bottom=insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            page.setPadding(0,dp(context,8),0,Math.max(dp(context,24),bottom+dp(context,8))); return insets;
        });
        Runnable refresh=new Runnable() { public void run() {
            boolean active=timer.active() || trackEnd.active(); status.setText(active?SleepTimer.status():"");
            status.setVisibility(active?View.VISIBLE:View.GONE); stop.setVisibility(active?View.VISIBLE:View.GONE);
            if(((View)panel).isAttachedToWindow()) ((View)panel).postDelayed(this,1000);
        }};
        ((View)panel).addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) { refresh.run(); view.requestApplyInsets(); }
            public void onViewDetachedFromWindow(View view) { ((View)panel).removeCallbacks(refresh); }
        }); SurfaceMotion.enter((View)panel,false,false); return panel;
    }
    public static void show(Context context) { show(context,null); }
    public static void show(Context context,View ignored) {
        Activity activity=activity(context); if(activity==null || activity.isFinishing()) return;
        Dialog dialog=new Dialog(activity); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); showInDialog(dialog,activity);
    }
    /** Reuses the queue window, preserving its native DialogFragment dismissal. */
    public static void showInDialog(Dialog dialog,Activity activity) {
        Dialog previous=visible.get(); if(previous!=null && previous!=dialog) previous.dismiss(); visible=new WeakReference<>(dialog);
        View source=PlayerWindow.source(activity);
        PlayerWindow.Snapshot snapshot=source.getWidth()>0 && source.getHeight()>0 ? new PlayerWindow.Snapshot(source) : null;
        LiquidMenuSurface glass=new LiquidMenuSurface(activity,snapshot);
        FrameLayout root=new FrameLayout(activity);
        if(snapshot!=null) root.addView(snapshot.view(activity,true),new FrameLayout.LayoutParams(-1,-1));
        root.addView(content(activity,glass,dialog::dismiss),new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) {}
            public void onViewDetachedFromWindow(View view) { glass.release(); if(snapshot!=null) snapshot.release(); }
        });
        View existing=dialog.findViewById(android.R.id.content);
        if(dialog.isShowing() && existing instanceof ViewGroup) {
            // Window.setContentView starts a native Scene transition which moves
            // detached Compose queue views into an ownerless transition overlay.
            // Replace the content directly in the current window instead.
            android.transition.TransitionManager.endTransitions((ViewGroup)existing);
            ((ViewGroup)existing).removeAllViews();
            ((ViewGroup)existing).addView(root,new ViewGroup.LayoutParams(-1,-1));
        } else { dialog.setContentView(root); dialog.show(); }
        Window window=dialog.getWindow(); if(window!=null) {
            SystemBarTheme.installWindow(window); window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); window.setLayout(-1,-1);
        }
    }
}
