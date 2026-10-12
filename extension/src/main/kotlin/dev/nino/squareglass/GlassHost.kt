/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import android.view.WindowInsetsController

/** Install the native scene bridge without changing native root measurement or content. */
object GlassHost {
    @JvmStatic
    fun wrapNative(activity: Activity, content: @Composable () -> Unit): @Composable () -> Unit {
        SquareSettings.initialize(activity)
        val page = activity.findViewById<android.view.View>(0x7f0b02e6)
        NativeBackdrop.attachNative(page)
        SystemBarTheme.install(activity)
        PageCoordinator.install(activity)
        if (activity.packageName == "dev.nino.tidalglass.test") ProbeDiagnostics.install(activity, page)
        return {
            val dark = NativeGlass.dark
            SideEffect {
                val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                activity.window.insetsController?.setSystemBarsAppearance(if (dark) 0 else mask, mask)
            }
            content()
        }
    }
}
