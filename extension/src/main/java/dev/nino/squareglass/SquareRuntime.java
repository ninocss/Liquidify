/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass;

import android.content.Context;
import android.content.res.Configuration;

/** Theme override is scoped to TIDAL's Activity, never to the device. */
public final class SquareRuntime {
    private SquareRuntime() {}
    public static Context context(Context base) {
        SquareSettings.initialize(base);
        NativePlayback.initialize(base);
        if (SquareSettings.themeEnabled() != 0) {
            // AppCompat reapplies its own night qualifier after attachBaseContext.
            // Keep its delegate in sync so XML and Compose select the same palette.
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    SquareSettings.themeMode() == 0 ? -1 : SquareSettings.themeMode());
        }
        android.app.LocaleManager languages=base.getSystemService(android.app.LocaleManager.class);
        if(languages!=null && !"en".equals(languages.getApplicationLocales().toLanguageTags()))
            languages.setApplicationLocales(android.os.LocaleList.forLanguageTags("en"));
        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(java.util.Locale.ENGLISH);
        if (SquareSettings.themeEnabled() != 0 && SquareSettings.themeMode() != 0) config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) |
                (SquareSettings.themeMode() == 2 ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return base.createConfigurationContext(config);
    }
}
