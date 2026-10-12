/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass;

import androidx.compose.ui.graphics.ColorKt;

/** Native Wave switch motion/gestures are retained; paint roles stay distinct. */
public final class SquareSwitchPaint {
    private SquareSwitchPaint() {}
    public static long thumbEnabled() { return ColorKt.Color(SquarePalette.isDark() ? 0xff0d0e11 : 0xffffffff); }
    public static long thumbDisabled() { return ColorKt.Color(SquarePalette.isDark() ? 0xffc7cbd4 : 0xffffffff); }
    public static long trackOff() { return ColorKt.Color(SquarePalette.isDark() ? 0xff737987 : 0xff828896); }
    public static long trackOn() { return ColorKt.Color(SquarePalette.ink(SquarePalette.isDark())); }
    public static long trackDisabled() { return ColorKt.Color(SquarePalette.isDark() ? 0xff454951 : 0xffb4b8c4); }
}
