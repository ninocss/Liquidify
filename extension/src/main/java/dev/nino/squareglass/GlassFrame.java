/* GPL-3.0-or-later. A background never determines wrap-content height. */
package dev.nino.squareglass;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public final class GlassFrame extends FrameLayout {
    public GlassFrame(Context context) { super(context); }
    @Override protected void onMeasure(int width,int height) {
        View paint=((ViewGroup)this).getChildCount()>0 ? ((ViewGroup)this).getChildAt(0) : null;
        if(paint==null) { super.onMeasure(width,height); return; }
        LayoutParams params=(LayoutParams)paint.getLayoutParams();
        int originalWidth=params.width,originalHeight=params.height;
        params.width=0; params.height=0;
        super.onMeasure(width,height);
        params.width=originalWidth; params.height=originalHeight;
        paint.measure(View.MeasureSpec.makeMeasureSpec(((View)this).getMeasuredWidth(),View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(((View)this).getMeasuredHeight(),View.MeasureSpec.EXACTLY));
    }
}
