/* GPL-3.0-or-later. A toolbar scrim with a transparent lower edge. */
package dev.nino.squareglass;
import android.graphics.*;
import android.graphics.drawable.Drawable;
public final class HeaderFade extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int tone; private float amount;
    public HeaderFade(int tone) { this.tone=tone; }
    public float progress() {return amount;}
    public void update(int color,float progress) {
        if(tone==color && amount==progress) return;
        tone=color; amount=progress; shader(); ((Drawable)this).invalidateSelf();
    }
    private void shader() {
        Rect b=((Drawable)this).getBounds();int rgb=tone&0xffffff;
        paint.setShader(new LinearGradient(0,b.top,0,Math.max(b.top+1,b.bottom),
            new int[]{tone,tone,rgb},new float[]{0,.90f,1},Shader.TileMode.CLAMP));
        paint.setAlpha(Math.round(amount*255));
    }
    @Override protected void onBoundsChange(Rect b) { shader(); }
    @Override public void draw(Canvas canvas) { if(amount>0) canvas.drawRect(((Drawable)this).getBounds(),paint); }
    @Override public void setAlpha(int alpha) { paint.setAlpha(Math.round(amount*255)); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
