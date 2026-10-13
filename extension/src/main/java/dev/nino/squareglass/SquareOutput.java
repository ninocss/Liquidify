/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.*;

/** Native manager remains authoritative. No OpenHome playback is enabled here. */
public final class SquareOutput {
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final WeakHashMap<View,Rect> anchors = new WeakHashMap<>();
    private static Session current;
    private SquareOutput() {}
    public static void anchor(View view,int l,int t,int r,int b) { anchors.put(view,new Rect(l,t,r,b)); }
    public static void clearAnchor(View view) { anchors.remove(view); }
    public static void changed() { main.post(() -> {if(current!=null) current.schedule();}); }
    public static void linnFailure(String message) {main.post(() -> {if(current!=null){current.selection.failed();current.message=message;current.draw();current.fit();}});}
    public static void linnProgress(String message) {main.post(() -> {if(current!=null && current.selection.target()!=null){current.selection.begin(current.selection.target(),SystemClock.uptimeMillis());current.message=message;current.draw();current.fit();}});}
    public static void result(Object token,Object value,boolean error) {
        Request request=(Request)token;
        main.post(() -> request.session.received(request.revision,value,error));
    }
    public static boolean showing(Dialog dialog) { return dialog!=null && current!=null && current.dialog==dialog; }
    public static void dismissed(Dialog dialog) { if(showing(dialog)) current.close(); }
    /** Settings entry is available before a native player/queue dialog exists. */
    public static void showFromSettings(Activity activity) {
        if(activity==null || activity.isFinishing() || activity.isDestroyed())return;
        if(current!=null)current.close();
        View parent=activity.getWindow().getDecorView();
        Rect frame=new Rect();parent.getWindowVisibleDisplayFrame(frame);
        float density=activity.getResources().getDisplayMetrics().density;
        Rect anchor=new Rect(frame.right-(int)(48*density),frame.top,
            frame.right-(int)(24*density),frame.top+(int)(48*density));
        Session session=new Session(null,activity,anchor,parent);current=session;
        try{session.open();}catch(RuntimeException error){session.close();throw error;}
    }
    public static boolean show(Dialog dialog) {
        if(SquareSettings.themeEnabled()==0) return false;
        Activity activity=dialog.getOwnerActivity();
        if(activity==null || activity.isFinishing() || activity.isDestroyed()) return true;
        if(current!=null) current.close();
        Rect anchor=null;
        View parent=PlayerWindow.source(activity);
        for(Map.Entry<View,Rect> entry:anchors.entrySet()) {
            View view=entry.getKey();
            if(view.isAttachedToWindow() && view.isShown() && (view.getRootView()==activity.getWindow().getDecorView()
                    || view.getRootView()==PlayerWindow.source(activity).getRootView())) {
                anchor=new Rect(entry.getValue());
                parent=view;
            }
        }
        // A missing icon geometry still opens a popup within this window.
        if(anchor==null) { Rect frame=new Rect();activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
            anchor=new Rect(frame.right-48,frame.top+48,frame.right-24,frame.top+72); }
        Session session=new Session(dialog,activity,anchor,parent);current=session;
        try { session.open(); } catch(RuntimeException error) {session.close();throw error;}
        return true;
    }
    private static final class Request {
        final Session session; final int revision;
        Request(Session s,int r) {session=s;revision=r;}
    }
    private static final class Session implements Application.ActivityLifecycleCallbacks {
        final Dialog dialog; final Activity activity; final Rect anchor; final View parent;
        final OutputSelection selection=new OutputSelection();
        final ArrayList<Object> devices=new ArrayList<>();
        final Runnable poll=() -> request(false);
        final Runnable timeout=() -> fetchFailed("Device search timed out. Tap Refresh to retry.");
        PopupWindow popup; FrameLayout shell; LinearLayout content,activeBox,rows;
        ScrollView scroll; TextView refreshButton,status; LiquidMenuSurface glass;
        Object listener,request,retryDevice; boolean closed,fetching,searching=true; String message="";
        PlayerWindow.Snapshot snapshot;
        int width,maxHeight;
        String drawn="";
        Session(Dialog d,Activity a,Rect bounds,View p) {dialog=d;activity=a;anchor=bounds;parent=p;}
        int dp(float value) {return Math.round(value*activity.getResources().getDisplayMetrics().density);}
        TextView text(String label,int size) {
            TextView view=new TextView(activity);view.setText(label);view.setTextSize(size);
            view.setTextColor(SquarePalette.ink(SquarePalette.isDark()));
            view.setTypeface(activity.getResources().getFont(0x7f090005));
            view.setPadding(dp(16),dp(8),dp(16),dp(8));return view;
        }
        void open() {
            if(dialog!=null)NativeOutputBridge.discard(dialog);
            Rect frame=new Rect();activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
            width=Math.max(1,Math.min(dp(320),frame.width()-dp(24)));
            maxHeight=Math.max(1,frame.height()-dp(24));
            shell=new FrameLayout(activity);snapshot=PlayerWindow.take();glass=new LiquidMenuSurface(activity,snapshot);
            glass.paint(shell,24f,false);
            content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);
            shell.addView(content,new FrameLayout.LayoutParams(-1,-2));
            LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);
            TextView title=text("Output",20);title.setAccessibilityHeading(true);
            header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            refreshButton=text("↻",26);refreshButton.setGravity(Gravity.CENTER);refreshButton.setMinHeight(dp(48));
            refreshButton.setContentDescription("Refresh output devices");refreshButton.setFocusable(true);
            header.addView(refreshButton,new LinearLayout.LayoutParams(dp(56),-2));
            refreshButton.setOnClickListener(v -> {message="";NativeOutputBridge.scan();refresh();});content.addView(header);
            activeBox=new LinearLayout(activity);activeBox.setOrientation(LinearLayout.VERTICAL);content.addView(activeBox);
            status=text("Searching…",14);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            status.setMinHeight(dp(48));status.setOnClickListener(v -> {if(retryDevice!=null) choose(retryDevice);else refresh();});
            content.addView(status);
            scroll=new ScrollView(activity);scroll.setFillViewport(false);
            rows=new LinearLayout(activity);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);
            content.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
            popup=new PopupWindow(shell,width,-2,true);popup.setOutsideTouchable(true);
            popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));popup.setElevation(dp(10));
            popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);popup.setClippingEnabled(true);
            popup.setOnDismissListener(this::close);
            activity.getApplication().registerActivityLifecycleCallbacks(this);
            listener=NativeOutputBridge.open();NativeOutputBridge.scan();
            draw();fit();SurfaceMotion.enter(shell,true,false);refresh();
        }
        void schedule() {if(!closed) {main.removeCallbacks(poll);main.postDelayed(poll,100);}}
        void refresh() {request(true);}
        void request(boolean visible) {
            if(closed)return;
            main.removeCallbacks(poll);main.removeCallbacks(timeout);
            NativeOutputBridge.cancel(request);request=null;
            fetching=true;
            if(visible) {searching=true;refreshButton.setText("…");refreshButton.setContentDescription("Searching output devices. Refresh again");draw();}
            int revision=selection.nextRequest();
            main.postDelayed(timeout,8000);
            request=NativeOutputBridge.request(new Request(this,revision));
        }
        void received(int revision,Object value,boolean error) {
            if(closed || !selection.accepts(revision)) return;
            if(error || !(value instanceof List)) {fetchFailed("Device search failed. Tap Refresh to retry.");return;}
            main.removeCallbacks(timeout);fetching=false;searching=false;refreshButton.setText("↻");
            refreshButton.setContentDescription("Refresh output devices");
            LinkedHashMap<String,Object> unique=new LinkedHashMap<>();
            for(Object item:(List<?>)value) if(item!=null) unique.put(NativeOutputBridge.id(item),item);
            devices.clear();devices.addAll(unique.values());
            Object active=NativeOutputBridge.active();
            LinnProvider linn=LinnProvider.instance;
            if(linn!=null && linn.connecting && selection.target()!=null && selection.target().startsWith("LINN_")){
                selection.begin(selection.target(),SystemClock.uptimeMillis());message=linn.status;
            }
            int result=selection.observe(active==null?null:NativeOutputBridge.id(active),SystemClock.uptimeMillis());
            if(result==1) {message="";retryDevice=null;}
            if(result==-1) message="Connection was not confirmed. Tap here to retry.";
            // The provider may finish after our timeout. Its confirmed state wins.
            if(active!=null && retryDevice!=null && Objects.equals(NativeOutputBridge.id(active),NativeOutputBridge.id(retryDevice))) {
                message="";retryDevice=null;
            }
            draw();main.postDelayed(poll,1000);
        }
        void fetchFailed(String error) {
            if(closed)return;
            selection.invalidate();selection.failed();NativeOutputBridge.cancel(request);request=null;
            main.removeCallbacks(timeout);fetching=false;searching=false;refreshButton.setText("↻");
            refreshButton.setContentDescription("Refresh output devices");message=error;draw();fit();
        }
        void choose(Object device) {
            if(closed || selection.target()!=null) return;
            retryDevice=device;message="";selection.begin(NativeOutputBridge.id(device),SystemClock.uptimeMillis());
            draw();fit();
            try {NativeOutputBridge.select(device);schedule();}
            catch(RuntimeException error) {selection.failed();message="Connection failed. Tap here to retry.";draw();fit();}
        }
        void heading(LinearLayout box,String label) {TextView title=text(label,11);title.setAccessibilityHeading(true);box.addView(title);}
        String name(Object item) {return NativeOutputBridge.local(item)?"This phone":NativeOutputBridge.name(item);}
        String type(Object item) {String linn=LinnProvider.label(item);if(linn!=null)return linn;int resource=NativeOutputBridge.type(item);return resource>0?activity.getString(resource):"";}
        final class OutputGlyph extends View {
            final boolean phone;
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            OutputGlyph(boolean isPhone){super(activity);phone=isPhone;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
            void line(Canvas c,float x1,float y1,float x2,float y2){c.drawLine(x1,y1,x2,y2,paint);}
            @Override protected void onDraw(Canvas canvas){
                super.onDraw(canvas);
                float d=getResources().getDisplayMetrics().density;
                canvas.save();canvas.translate((getWidth()-32*d)/2f,(getHeight()-40*d)/2f);canvas.scale(d,d);
                paint.setColor(SquarePalette.ink(SquarePalette.isDark()));paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
                if(phone){
                    canvas.drawRoundRect(8,4,24,36,3.5f,3.5f,paint);
                    line(canvas,13,8,19,8);canvas.drawCircle(16,31.5f,0.8f,paint);
                }else{
                    // Compact hi-fi renderer: display, control keys and speaker drivers.
                    canvas.drawRoundRect(2,9,30,31,3,3,paint);
                    canvas.drawRoundRect(6,13,17,19,1,1,paint);
                    line(canvas,7,24,13,24);line(canvas,7,27,13,27);
                    canvas.drawCircle(22,20,3.1f,paint);canvas.drawCircle(22,20,0.8f,paint);
                    canvas.drawCircle(27,15,0.7f,paint);canvas.drawCircle(27,25,0.7f,paint);
                }
                paint.setStyle(Paint.Style.FILL);canvas.restore();
            }
        }
        void row(LinearLayout box,Object item,boolean active) {
            boolean pending=Objects.equals(selection.target(),NativeOutputBridge.id(item));
            LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(64));
            OutputGlyph icon=new OutputGlyph(NativeOutputBridge.local(item));
            row.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));
            LinearLayout labels=new LinearLayout(activity);labels.setOrientation(LinearLayout.VERTICAL);
            TextView title=text(name(item),16);title.setPadding(0,dp(8),0,0);labels.addView(title);
            TextView subtitle=text(pending?"Connecting…":type(item),12);subtitle.setPadding(0,0,0,dp(8));labels.addView(subtitle);
            row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            if(active) row.addView(text("✓",20));
            int ink=SquarePalette.ink(SquarePalette.isDark());GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(dp(12));
            row.setBackground(new RippleDrawable(ColorStateList.valueOf((ink&0xffffff)|0x18000000),null,mask));
            row.setContentDescription(name(item)+", "+type(item)+(active?", active":pending?", connecting":""));
            row.setFocusable(true);row.setEnabled(selection.target()==null && !active);row.setOnClickListener(v -> choose(item));box.addView(row);
        }
        void draw() {
            Object active=NativeOutputBridge.active();
            StringBuilder signature=new StringBuilder(message).append('|').append(searching).append('|').append(selection.target()).append('|')
                .append(active==null?"":NativeOutputBridge.id(active));
            for(Object item:devices)signature.append('|').append(NativeOutputBridge.id(item)).append(':').append(name(item)).append(':').append(type(item));
            if(active!=null)signature.append('|').append(name(active)).append(':').append(type(active));
            // Polling must not replace focused TalkBack rows or reset scroll position.
            if(signature.toString().equals(drawn))return;drawn=signature.toString();
            int oldScroll=scroll.getScrollY();
            activeBox.removeAllViews();rows.removeAllViews();
            if(active!=null) {heading(activeBox,"ACTIVE DEVICE");row(activeBox,active,true);}
            heading(rows,"AVAILABLE DEVICES");int count=0;
            for(Object item:devices) if(active==null || !Objects.equals(NativeOutputBridge.id(active),NativeOutputBridge.id(item))) {row(rows,item,false);count++;}
            if(count==0) rows.addView(text(searching?"Searching…":"No other devices found",14));
            TextView manual=text("Linn not found? Add by address",14);manual.setMinHeight(dp(48));manual.setFocusable(true);
            manual.setOnClickListener(v -> {
                EditText address=new EditText(activity);address.setSingleLine(true);address.setHint("Local IP or device description URL");address.setContentDescription("Linn local network address");
                new android.app.AlertDialog.Builder(activity).setTitle("Add Linn device").setView(address)
                    .setNegativeButton("Cancel",null).setPositiveButton("Find",(d,w) -> LinnProvider.addAddress(address.getText().toString().trim())).show();
            });rows.addView(manual);
            String pending=selection.target();
            status.setText(!message.isEmpty()?message:pending!=null?"Connecting…":searching?"Searching…":"");
            status.setVisibility(status.getText().length()==0?View.GONE:View.VISIBLE);
            status.setFocusable(!message.isEmpty());
            rows.post(() -> {if(!closed)scroll.scrollTo(0,oldScroll);});
            if(popup.isShowing())fit();
        }
        void fit() {
            Rect frame=new Rect();activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
            maxHeight=Math.max(1,frame.height()-dp(24));
            scroll.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
            // The decorative ComposeView can only compose after window attachment.
            // Native content alone determines the popup dimensions.
            content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(maxHeight,View.MeasureSpec.AT_MOST));
            int fixed=content.getMeasuredHeight()-scroll.getMeasuredHeight();
            scroll.getLayoutParams().height=Math.max(1,Math.min(scroll.getMeasuredHeight(),maxHeight-fixed));
            scroll.requestLayout();content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(maxHeight,View.MeasureSpec.AT_MOST));
            int height=content.getMeasuredHeight();
            int left=Math.max(frame.left+dp(12),Math.min(anchor.right-width,frame.right-width-dp(12)));
            int top=anchor.bottom+dp(12);if(top+height>frame.bottom-dp(12)) top=anchor.top-height-dp(12);
            top=Math.max(frame.top+dp(12),Math.min(top,frame.bottom-height-dp(12)));
            if(popup.isShowing())popup.update(left,top,width,height);
            // The full player has its own Dialog window. Attach above that window,
            // otherwise an Activity-token popup is hidden underneath the player.
            else popup.showAtLocation(parent,Gravity.TOP|Gravity.LEFT,left,top);
        }
        void close() {
            if(closed)return;closed=true;if(current==this)current=null;
            selection.invalidate();main.removeCallbacks(poll);main.removeCallbacks(timeout);
            NativeOutputBridge.cancel(request);request=null;
            if(listener!=null) {NativeOutputBridge.close(listener);listener=null;}
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
            if(popup!=null) {popup.setOnDismissListener(null);popup.dismiss();}
            if(glass!=null)glass.release();
            if(snapshot!=null) {snapshot.release();snapshot=null;}
        }
        public void onActivityPaused(Activity a) {if(a==activity)close();}
        public void onActivityDestroyed(Activity a) {if(a==activity)close();}
        public void onActivityCreated(Activity a,Bundle b) {}
        public void onActivityStarted(Activity a) {}
        public void onActivityResumed(Activity a) {}
        public void onActivityStopped(Activity a) {}
        public void onActivitySaveInstanceState(Activity a,Bundle b) {}
    }
}
