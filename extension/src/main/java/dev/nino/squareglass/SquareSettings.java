/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass;

public final class SquareSettings {
    private SquareSettings() {}
    private static android.content.SharedPreferences preferences;
    /** Manager placeholder remains a backwards-compatible initial value. */
    public static int initialTheme() { return 0; }
    public static int themeMode() { return integer("theme", initialTheme(), 0, 2); }
    public static void initialize(android.content.Context context) {
        if (preferences == null) {
            preferences = context.getApplicationContext().getSharedPreferences("squareglass_design_v1", 0);
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }
    private static final android.content.SharedPreferences.OnSharedPreferenceChangeListener listener =
            (store, key) -> DesignState.changed();
    public static int integer(String key, int fallback, int low, int high) {
        return Math.max(low, Math.min(high, preferences == null ? fallback : preferences.getInt(key, fallback)));
    }
    public static boolean flag(String key, boolean fallback) {
        return preferences == null ? fallback : preferences.getBoolean(key, fallback);
    }
    public static void put(String key, int value) { if (preferences != null) preferences.edit().putInt(key, value).apply(); }
    public static void put(String key, boolean value) { if (preferences != null) preferences.edit().putBoolean(key, value).apply(); }
    public static int preset() { return integer("preset", 0, 0, 2); }
    public static float blur() { return integer("blur", preset() == 1 ? 16 : 8, 0, 24); }
    public static float opacity() { return integer("opacity", preset() == 1 ? 65 : 50, 20, 90) / 100f; }
    public static float readableOpacity(boolean dark) { return Math.max(opacity(), dark ? .82f : .64f); }
    public static void reset() { if (preferences != null) preferences.edit().clear().apply(); }
    public static float refraction() { return integer("lens", preset() == 1 ? 0 : 40, 0, 100) / 100f; }
    public static void preset(int value) {
        if (preferences != null) preferences.edit().putInt("preset", value)
                .remove("blur").remove("opacity").remove("lens").apply();
    }
    public static boolean motion() {
        return !flag("reduceMotion", false) && android.animation.ValueAnimator.areAnimatorsEnabled();
    }
    public static boolean glass(String component) { return preset() != 2 && flag(component, true); }
    /** A ZIP placeholder makes old Managers reject packs needing resource support. */
    public static int managerReady() { return 1; }
    public static int themeEnabled() { return 1; }
}
