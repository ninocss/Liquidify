/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

object NativeTheme {
    @JvmStatic
    @Composable
    fun waveColors(): al0.a {
        DesignState.revision
        // Subscribe to Android configuration; a static native palette otherwise
        // survives the Activity's recreation when the system theme changes.
        val uiMode = LocalConfiguration.current.uiMode
        return remember(uiMode, SquareSettings.themeMode()) { al0.a() }
    }
}
