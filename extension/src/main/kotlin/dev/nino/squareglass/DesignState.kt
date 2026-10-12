/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import dev.nino.squareglass.material.GlassEffectConfig
import dev.nino.squareglass.material.GlassStyle

object DesignState {
    var revision by mutableIntStateOf(0)
        private set
    @JvmStatic fun changed() { revision++ }
    fun config(): GlassEffectConfig {
        revision
        return GlassEffectConfig(
            globalEnabled = SquareSettings.preset() != 2,
            blurRadius = SquareSettings.blur(),
            lensHeight = if (SquareSettings.motion()) SquareSettings.refraction() else 0f,
            lensAmount = if (SquareSettings.motion()) SquareSettings.refraction() else 0f,
            surfaceOpacity = SquareSettings.readableOpacity(SquarePalette.isDark()),
            style = if (SquareSettings.preset() == 1) GlassStyle.BLUR else GlassStyle.LIQUID,
            playerEnabled = SquareSettings.glass("player"),
            miniPlayerEnabled = SquareSettings.glass("mini"),
            navBarEnabled = SquareSettings.glass("nav"),
        )
    }
}
