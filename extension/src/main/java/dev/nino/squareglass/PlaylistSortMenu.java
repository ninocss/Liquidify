/* GPL-3.0-or-later. Native sort metadata and callbacks in an anchored glass menu. */
package dev.nino.squareglass;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.fragment.app.DialogFragment;

public final class PlaylistSortMenu extends Dialog {
    private final DialogFragment owner;
    private final String[] options;
    private final int selected;
    private LiquidMenuSurface glass;
    private FrameLayout shell;
    private ScrollView scroll;
    private boolean chosen;
    private PlaylistSortMenu(Activity activity, DialogFragment owner, String[] options, int selected) {
        super(activity); ((Dialog)this).setOwnerActivity(activity);
        this.owner=owner; this.options=options; this.selected=selected;
        ((Dialog)this).requestWindowFeature(Window.FEATURE_NO_TITLE); ((Dialog)this).setCanceledOnTouchOutside(true);
    }
    public static Dialog create(DialogFragment owner, Dialog original, String[] options, int selected) {
        String type=owner.getClass().getName();
        if (SquareSettings.themeEnabled()==0 || !(type.equals("jj.a") || type.equals("jj.b")) ||
            !(owner instanceof DialogInterface.OnClickListener) || owner.getActivity()==null || options==null) return original;
        return new PlaylistSortMenu(owner.requireActivity(),owner,options.clone(),selected);
    }
    private int dp(int n) { return Math.round(n*((Dialog)this).getContext().getResources().getDisplayMetrics().density); }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        shell=new FrameLayout(((Dialog)this).getContext());
        scroll=new ScrollView(((Dialog)this).getContext());
        LinearLayout rows=new LinearLayout(((Dialog)this).getContext()); rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(8),dp(8),dp(8),dp(8));
        int ink=SquarePalette.ink(SquarePalette.isDark());
        for(int i=0; i<options.length; i++) {
            final int index=i;
            CheckedTextView row=new CheckedTextView(((Dialog)this).getContext()); row.setText(options[i]);
            row.setTextSize(16); row.setTextColor(ink); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPaddingRelative(dp(16),dp(14),dp(16),dp(14)); row.setMinHeight(dp(52));
            row.setCheckMarkDrawable(android.R.drawable.btn_radio);
            row.setCheckMarkTintList(ColorStateList.valueOf(ink)); row.setChecked(i==selected);
            row.setOnClickListener(view -> {
                if(chosen) return; chosen=true;
                ((DialogInterface.OnClickListener)owner).onClick(this,index);
                dismiss();
            });
            rows.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        scroll.addView(rows,new ScrollView.LayoutParams(-1,-2));
        glass=new LiquidMenuSurface(((Dialog)this).getOwnerActivity(),null); glass.paint(shell,24f,false);
        shell.addView(scroll,new FrameLayout.LayoutParams(-1,-2)); ((Dialog)this).setContentView(shell);
        Window window=((Dialog)this).getWindow();
        if(window!=null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
    }
    @Override protected void onStart() {
        super.onStart();
        Rect frame=new Rect(); ((Dialog)this).getOwnerActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int width=Math.min(dp(320),frame.width()-dp(24));
        int maxHeight=Math.min(dp(560),frame.height()-dp(24));
        // Dialog.show calls onStart before attaching the window. Only measure
        // the native options here; measuring the shell would compose its glass
        // without a window recomposer and crash before the menu can appear.
        scroll.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(maxHeight,View.MeasureSpec.AT_MOST));
        int height=Math.min(maxHeight,scroll.getMeasuredHeight());
        Window window=((Dialog)this).getWindow(); if(window==null) return;
        window.setLayout(width,height);
        int id=((Dialog)this).getContext().getResources().getIdentifier("action_sort","id",((Dialog)this).getContext().getPackageName());
        View anchor=((Dialog)this).getOwnerActivity().findViewById(id);
        int[] position={frame.right-dp(24),frame.top+dp(56)};
        if(anchor!=null) { anchor.getLocationOnScreen(position); position[1]+=anchor.getHeight(); position[0]+=anchor.getWidth(); }
        WindowManager.LayoutParams params=window.getAttributes(); params.gravity=Gravity.TOP|Gravity.LEFT;
        params.x=Math.max(frame.left+dp(12),Math.min(position[0]-width,frame.right-width-dp(12)));
        params.y=Math.max(dp(12),Math.min(position[1]-frame.top+dp(8),frame.height()-height-dp(12)));
        window.setAttributes(params);
    }
    @Override public void dismiss() {
        super.dismiss(); if(glass!=null) { glass.release(); glass=null; }
    }
}
