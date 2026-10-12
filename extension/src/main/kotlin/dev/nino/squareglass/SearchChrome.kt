/* GPL-3.0-or-later. The real native search decoration; callbacks and geometry stay native. */
package dev.nino.squareglass
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
object SearchChrome {
    @JvmStatic fun container(): @Composable () -> Unit = {
        DesignState.revision
        Box(GlassPanels.panel(Modifier.fillMaxSize(),28f))
    }
    @JvmStatic fun shape(): Shape = RoundedCornerShape(28.dp)
    @JvmStatic fun cursor(): Brush = SolidColor(Color(SquarePalette.ink(SquarePalette.isDark())))
}
