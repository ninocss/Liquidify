/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp

/** Native content padding reserves the animated header/footer, but does not clip scroll paint. */
object SquareLyrics {
    @JvmStatic
    fun viewport(original: Modifier, padding: PaddingValues): Modifier = original
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
        val top = padding.calculateTopPadding().toPx().coerceIn(0f, size.height)
        val bottom = (size.height - padding.calculateBottomPadding().toPx()).coerceIn(top, size.height)
        clipRect(top = top, bottom = bottom) { this@drawWithContent.drawContent() }
        val height = bottom - top
        if (height > 0f) {
            val fade = (24.dp.toPx() / height).coerceAtMost(.5f)
            drawRect(
                brush = Brush.verticalGradient(
                    Pair(0f, Color.Transparent), Pair(fade, Color.Black),
                    Pair(1f - fade, Color.Black), Pair(1f, Color.Transparent),
                    startY = top, endY = bottom,
                ),
                topLeft = Offset(0f, top), size = Size(size.width, height),
                blendMode = BlendMode.DstIn,
            )
        }
    }
}
