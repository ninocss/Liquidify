/* GPL-3.0-or-later. Read-only progress from TIDAL's authoritative player. */
package dev.nino.squareglass

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay

object MiniProgress {
    @Composable fun decorate(original: Modifier): Modifier {
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
        val view = LocalView.current
        var fraction by remember { mutableFloatStateOf(-1f) }
        val ink = Color(SquarePalette.ink(NativeGlass.dark))
        LaunchedEffect(lifecycle,view) {
            while (true) {
                fraction = if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && view.isShown) {
                    try {
                        val player = NativePlayback.host()
                        val duration = player?.currentMediaDuration ?: 0
                        if (duration > 0 && player?.currentItem != null)
                            PlaybackProgress.fraction(player.currentMediaPosition,duration) else -1f
                    } catch (_: RuntimeException) { -1f }
                } else -1f
                delay(250)
            }
        }
        // Deliberately no pointer handler or accessibility stop: this line is
        // informational. All seeking remains in the native full player.
        return original.drawWithContent {
            drawContent()
            if (fraction >= 0f) {
                val inset = 24.dp.toPx()
                val width = (size.width - 2 * inset).coerceAtLeast(0f)
                val y = size.height - 5.dp.toPx()
                drawLine(ink.copy(alpha=.12f),Offset(inset,y),Offset(inset+width,y),1.5.dp.toPx(),StrokeCap.Round)
                if (fraction > 0f) drawLine(ink.copy(alpha=.72f),Offset(inset,y),Offset(inset+width*fraction,y),1.5.dp.toPx(),StrokeCap.Round)
            }
        }
    }
}
