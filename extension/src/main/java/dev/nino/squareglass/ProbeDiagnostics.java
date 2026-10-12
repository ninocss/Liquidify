package dev.nino.squareglass;

import android.app.Activity;
import android.content.*;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;

/** Local test builds only. Exposes counters, never account, library or playback data. */
public final class ProbeDiagnostics {
    public static int nativeCaptures;
    public static int accessoryFreezes;
    public static int accessoryHandovers;
    public static long accessoryFirstCaptureUptime, navFirstSampleDrawUptime, miniFirstSampleDrawUptime;
    public static int nativeDrawTraversals, accessoryFirstCaptureTraversal, navFirstSampleTraversal, miniFirstSampleTraversal;
    public static int accessoryRenderStatus, accessorySampleAlpha;
    public static int glassDraws;
    public static int nativeTabActions;
    public static int nativeBackActions;
    public static int idleAnimations, likeAnimations;
    public static boolean enabled;
    public static int gestureStarts, gestureCommits, gestureRequests;
    public static int wavePaletteCalls;
    public static int marketPaletteCalls;
    public static long hostBackground;
    private static final java.util.Map<String,String> paletteValues = new java.util.LinkedHashMap<>();
    public static void palette(String role, int original, int changed) {
        if (!enabled || !(role.startsWith("surface") || role.startsWith("text"))) return;
        paletteValues.put(role, Integer.toHexString(original) + ">" + Integer.toHexString(changed));
    }
    private static void styles(View view, StringBuilder out, int depth) {
        String name = "none";
        try { if (view.getId() != View.NO_ID) name = view.getResources().getResourceEntryName(view.getId()); }
        catch (android.content.res.Resources.NotFoundException ignored) {}
        if (depth < 14) {
            out.append(depth).append(':').append(view.getClass().getSimpleName()).append('/').append(name);
            if (view.getBackground() instanceof ColorDrawable)
                out.append(" bg=").append(Integer.toHexString(((ColorDrawable)view.getBackground()).getColor()));
            if(view.getBackground() instanceof HeaderFade)out.append(" fade=").append(((HeaderFade)view.getBackground()).progress());
            if (view instanceof TextView) out.append(" textColor=").append(Integer.toHexString(((TextView)view).getCurrentTextColor()));
            int[] position=new int[2];view.getLocationInWindow(position);
            out.append(" @").append(position[0]).append(',').append(position[1]).append(' ').append(view.getWidth()).append('x').append(view.getHeight()).append(" visibility=").append(view.getVisibility()).append(" paddingTop=").append(view.getPaddingTop());
            if(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p=(ViewGroup.MarginLayoutParams)view.getLayoutParams();
                out.append(" margins=").append(p.topMargin).append(',').append(p.bottomMargin);
            }
            out.append('\n');
        }
        if (view instanceof ViewGroup) {
            var group=(ViewGroup)view;
            for (int i=0;i<group.getChildCount();i++) styles(group.getChildAt(i),out,depth+1);
        }
    }
    private static void report(BroadcastReceiver receiver, Context context) {
        receiver.setResultCode(0);
        receiver.setResultData("nativeCaptures=" + nativeCaptures + ";glassDraws=" + glassDraws
                + ";accessoryHandovers=" + accessoryHandovers + ";accessoryFirstCaptureUptime=" + accessoryFirstCaptureUptime
                + ";navFirstSampleDrawUptime=" + navFirstSampleDrawUptime + ";miniFirstSampleDrawUptime=" + miniFirstSampleDrawUptime
                + ";nativeDrawTraversals=" + nativeDrawTraversals + ";accessoryFirstCaptureTraversal=" + accessoryFirstCaptureTraversal
                + ";navFirstSampleTraversal=" + navFirstSampleTraversal + ";miniFirstSampleTraversal=" + miniFirstSampleTraversal
                + ";accessoryFreezes=" + accessoryFreezes + ";accessoryRenderStatus=" + accessoryRenderStatus + ";accessorySampleAlpha=" + accessorySampleAlpha + ";nativeTabActions=" + nativeTabActions + ";nativeBackActions=" + nativeBackActions
                + ";appUiMode=" + context.getResources().getConfiguration().uiMode
                + ";systemUiMode=" + android.content.res.Resources.getSystem().getConfiguration().uiMode
                + ";paletteDark=" + SquarePalette.isDark()
                + ";gestureStarts=" + gestureStarts + ";gestureCommits=" + gestureCommits + ";gestureRequests=" + gestureRequests
                + ";idleAnimations=" + idleAnimations + ";likeAnimations=" + likeAnimations
                + ";wavePaletteCalls=" + wavePaletteCalls + ";marketPaletteCalls=" + marketPaletteCalls
                + ";hostBackground=" + Long.toHexString(hostBackground) + ";palettes=" + paletteValues);
    }
    private static void reportStyles(BroadcastReceiver receiver, Activity activity) {
        var output=new StringBuilder();
        styles(activity.getWindow().getDecorView(),output,0);
        receiver.setResultData(output.toString());
    }
    public static void install(Activity activity, View lifetime) {
        enabled = true;
        // Only the explicitly scoped test package installs this receiver. Keep
        // that foreground QA window awake; this never disables the lock screen.
        activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("dev.nino.squareglass.STYLES")) {
                    reportStyles(this, activity);
                } else report(this, context);
            }
        };
        var filter=new IntentFilter("dev.nino.squareglass.STATUS");
        filter.addAction("dev.nino.squareglass.STYLES");
        activity.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        lifetime.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) {
                activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                activity.unregisterReceiver(receiver);
                lifetime.removeOnAttachStateChangeListener(this);
            }
        });
    }
}
