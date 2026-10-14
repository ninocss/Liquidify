/* GPL-3.0-or-later. AndroidLiquidGlass interaction material; no native callbacks. */
package dev.nino.squareglass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.nino.squareglass.backdrop.Backdrop
import dev.nino.squareglass.backdrop.backdrops.emptyBackdrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nino.squareglass.backdrop.drawBackdrop
import dev.nino.squareglass.backdrop.effects.blur
import dev.nino.squareglass.backdrop.effects.lens
import dev.nino.squareglass.backdrop.effects.vibrancy
import dev.nino.squareglass.backdrop.highlight.Highlight
import dev.nino.squareglass.material.InteractiveHighlight

object GlassMotion {
    /** Preserve the host's non-composable bounce-press ABI. */
    @JvmStatic fun bounce(original: Modifier): Modifier = original.composed { control(Modifier) }

    @Composable fun control(original: Modifier): Modifier {
        val scope = rememberCoroutineScope()
        val highlight = remember(scope) { InteractiveHighlight(scope) }
        return feedback(original, highlight, NativeBackdrop.active).then(highlight.gestureModifier)
    }

    @Composable
    fun feedback(original: Modifier, highlight: InteractiveHighlight, backdrop: Backdrop = emptyBackdrop()): Modifier {
        DesignState.revision
        val dark = NativeGlass.dark
        val ink = Color(SquarePalette.ink(dark))
        return original.drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            effects = {
                val progress = if (SquareSettings.motion()) highlight.pressProgress.coerceIn(0f, 1f) else 0f
                if (progress > .001f && SquareSettings.preset() != 2) {
                    vibrancy()
                    blur(2.dp.toPx() * progress)
                    if (SquareSettings.preset() == 0) {
                        val reach = size.minDimension * .2f * progress * SquareSettings.refraction()
                        lens(reach, reach * 1.4f)
                    }
                }
            },
            highlight = {
                Highlight.Default.copy(alpha = if (SquareSettings.motion() && SquareSettings.preset() != 2)
                    highlight.pressProgress.coerceIn(0f, 1f) else 0f)
            },
            shadow = null,
            layerBlock = {
                val progress = if (SquareSettings.motion()) highlight.pressProgress else 0f
                scaleX = 1f + .08f * progress
                scaleY = 1f - .04f * progress
            },
            onDrawBackdrop = { drawContent ->
                if (SquareSettings.motion() && highlight.pressProgress > .001f && SquareSettings.preset() != 2) drawContent()
            },
            onDrawSurface = {
                val progress = if (SquareSettings.motion()) highlight.pressProgress.coerceIn(0f, 1f) else 0f
                drawRect(ink.copy(alpha = .06f * progress))
            }
        ).then(highlight.modifier)
    }
}
