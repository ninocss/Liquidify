/* GPL-3.0-or-later. Layout-owned edge-to-edge geometry, without moving native transitions. */
package dev.nino.squareglass;
import android.view.*;
public final class PageInsets {
    private PageInsets() {}
    public static void top(View root,int status) {
        if(root.getPaddingTop()!=0)root.setPadding(root.getPaddingLeft(),0,root.getPaddingRight(),root.getPaddingBottom());
        ViewGroup.LayoutParams p=root.getLayoutParams();
        if(p instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m=(ViewGroup.MarginLayoutParams)p;
            if(m.topMargin>0 && m.topMargin<=status) {
                m.topMargin=0;root.setLayoutParams(p);
            }
        }
    }
}
