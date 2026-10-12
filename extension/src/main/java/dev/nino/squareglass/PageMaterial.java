/* GPL-3.0-or-later. Page-local glass film, with no recursive backdrop capture. */
package dev.nino.squareglass;
import android.view.View;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
public final class PageMaterial {
    private PageMaterial() {}
    public static android.graphics.drawable.Drawable background(View view,int tone,int radius) {
        boolean dark=SquarePalette.isDark();float d=view.getResources().getDisplayMetrics().density;
        int base=tone==0?SquarePalette.base(dark):tone;
        GradientDrawable film=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{(CoverColors.blend(base,dark?0xff555865:0xffffffff,.58f)&0xffffff)|0xd9000000,(CoverColors.blend(base,dark?0xff282b35:0xffffffff,.3f)&0xffffff)|0xb8000000});
        film.setCornerRadius(radius*d);film.setStroke(Math.max(1,Math.round(d)),dark?0x38ffffff:0xe6ffffff);
        if(SquareSettings.preset()==2)film.setColor(SquarePalette.surface(dark));
        StateListDrawable states=new StateListDrawable();
        GradientDrawable selected=new GradientDrawable();selected.setColor(CoverColors.blend(base,SquarePalette.ink(dark),.15f));
        selected.setCornerRadius(radius*d);selected.setStroke(Math.max(1,Math.round(d)),dark?0x80ffffff:0xccffffff);
        states.addState(new int[]{android.R.attr.state_selected},selected);
        states.addState(new int[]{android.R.attr.state_activated},selected);
        states.addState(new int[]{android.R.attr.state_checked},selected);
        states.addState(new int[]{},film);
        return new RippleDrawable(ColorStateList.valueOf((SquarePalette.ink(dark)&0xffffff)|0x18000000),states,null);
    }
}
