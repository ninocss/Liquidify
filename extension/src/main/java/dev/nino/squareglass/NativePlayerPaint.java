/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass;

import androidx.compose.ui.graphics.ColorKt;

/** Adapt the native cover-derived player paint; never modify the cover itself. */
public final class NativePlayerPaint {
    private NativePlayerPaint() {}
    public static com.tidal.android.feature.playerscreen.ui.model.PlayerBackgroundStyle style(
            com.tidal.android.feature.playerscreen.ui.model.PlayerBackgroundStyle original) {
        return SquareSettings.glass("player") ?
            com.tidal.android.feature.playerscreen.ui.model.PlayerBackgroundStyle.DARK_BLUR :
            com.tidal.android.feature.playerscreen.ui.model.PlayerBackgroundStyle.FILLED_COLOR;
    }
    public static long background(long original) {
        int seed = (int)(original >>> 32);
        boolean dark = SquarePalette.isDark();
        int base = SquarePalette.base(dark);
        float mix = dark ? .70f : .92f;
        int r = Math.round(((seed >>> 16) & 255) * (1 - mix) + ((base >>> 16) & 255) * mix);
        int g = Math.round(((seed >>> 8) & 255) * (1 - mix) + ((base >>> 8) & 255) * mix);
        int b = Math.round((seed & 255) * (1 - mix) + (base & 255) * mix);
        return ColorKt.Color(0xff000000 | r << 16 | g << 8 | b);
    }
    public static long base() { return ColorKt.Color(SquarePalette.base(SquarePalette.isDark())); }
    public static long film() { return ColorKt.Color(SquarePalette.isDark() ? 0xff000000 : 0xffffffff); }
    public static long ink() { return ColorKt.Color(SquarePalette.ink(SquarePalette.isDark())); }
    public static long foreground(long original) {
        int alpha = (int)(original >>> 56);
        // Preserve disabled states; readable secondary labels need a stronger ink.
        if (alpha > 97) alpha = Math.max(alpha, 179);
        return ColorKt.Color((SquarePalette.ink(SquarePalette.isDark()) & 0xffffff) | alpha << 24);
    }
}
