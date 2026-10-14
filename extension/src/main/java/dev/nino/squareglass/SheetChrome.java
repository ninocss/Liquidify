/* GPL-3.0-or-later. One presentation surface; native dialogs own actions and dismissal. */
package dev.nino.squareglass;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public final class SheetChrome {
    private static final String MARKER = "liquidify.sheet.v1";
    private SheetChrome() {}
    private static int dp(View view, int n) { return Math.round(n * view.getResources().getDisplayMetrics().density); }
    public static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity)context;
            Context next = ((ContextWrapper)context).getBaseContext();
            if (next == context) break;
            context = next;
        }
        return context instanceof Activity ? (Activity)context : null;
    }
    public static void hideHandles(View view) {
        if (view.getId() != View.NO_ID) {
            try {
                String name = view.getResources().getResourceEntryName(view.getId());
                if (name.equals("handle") || name.equals("drag_handle") || name.equals("dragHandle") ||
                    name.equals("bottom_sheet_handle") || name.equals("bottomSheetHandle") ||
                    name.equals("topDivider") || name.equals("top_divider") ||
                    name.equals("shadowView")) view.setVisibility(View.GONE);
            } catch (android.content.res.Resources.NotFoundException ignored) {}
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i=0; i<group.getChildCount(); i++) hideHandles(group.getChildAt(i));
        }
    }
    public static LinearLayout layout(FrameLayout shell, View content) {
        hideHandles(content);
        LinearLayout column = new LinearLayout(shell.getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setTag(MARKER);
        FrameLayout gripArea = new FrameLayout(shell.getContext());
        View grip = new View(shell.getContext());
        GradientDrawable paint = new GradientDrawable();
        paint.setColor((SquarePalette.ink(SquarePalette.isDark()) & 0xffffff) | 0x60000000);
        paint.setCornerRadius(dp(shell, 2)); grip.setBackground(paint);
        grip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        gripArea.addView(grip, new FrameLayout.LayoutParams(dp(shell,36),dp(shell,4),Gravity.CENTER));
        column.addView(gripArea,new LinearLayout.LayoutParams(-1,dp(shell,28)));
        column.addView(content,new LinearLayout.LayoutParams(-1,-2));
        shell.addView(column,new FrameLayout.LayoutParams(-1,-2));
        return column;
    }
    /** Shared Material dialog hook, scoped to TIDAL-owned dialogs. */
    public static void opened(Dialog dialog) {
        if (SquareSettings.themeEnabled()==0 || SquareMenus.owns(dialog) || dialog.getWindow()==null) return;
        String type = dialog.getClass().getName();
        if (type.startsWith("com.onetrust.") || type.startsWith("com.google.android.gms.")) return;
        Activity activity = dialog.getOwnerActivity();
        if (activity==null) activity = activity(dialog.getContext());
        if (activity==null || activity.isFinishing()) return;
        int id = dialog.getContext().getResources().getIdentifier("design_bottom_sheet","id",dialog.getContext().getPackageName());
        View candidate = dialog.findViewById(id);
        if (!(candidate instanceof FrameLayout)) return;
        FrameLayout sheet=(FrameLayout)candidate;
        View prior=sheet.findViewWithTag(MARKER);
        if (prior instanceof LinearLayout) {
            if (prior.isAttachedToWindow()) return;
            // Rebuild only on the next show. Mutating an ancestor from inside
            // dispatchDetachedFromWindow can reattach native children mid-detach.
            View oldContent=((LinearLayout)prior).getChildAt(1);
            if (!(oldContent instanceof FrameLayout)) return;
            FrameLayout nativeContent=(FrameLayout)oldContent;
            sheet.removeAllViews();
            while(nativeContent.getChildCount()>0) {
                View child=nativeContent.getChildAt(0); ViewGroup.LayoutParams params=child.getLayoutParams();
                nativeContent.removeViewAt(0); sheet.addView(child,params);
            }
        }
        if (sheet.getChildCount()==0) return;
        // Keep the behavior's own container and callbacks. Reparent before show,
        // never call Window.setContentView on an already showing dialog.
        FrameLayout content = new FrameLayout(sheet.getContext());
        while(sheet.getChildCount()>0) {
            View child=sheet.getChildAt(0); ViewGroup.LayoutParams params=child.getLayoutParams();
            sheet.removeViewAt(0); content.addView(child,params);
        }
        sheet.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout shell=new FrameLayout(sheet.getContext());
        LiquidMenuSurface glass=new LiquidMenuSurface(activity,null);
        glass.paint(shell,28f,false,true);
        layout(shell,content);
        sheet.addView(shell,new FrameLayout.LayoutParams(-1,-2));
        shell.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) {}
            public void onViewDetachedFromWindow(View view) {
                glass.release(); view.removeOnAttachStateChangeListener(this);
            }
        });
    }
}
