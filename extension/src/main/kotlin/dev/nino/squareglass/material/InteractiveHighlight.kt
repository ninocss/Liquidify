/* Adapted from the pinned Square source; GPL-3.0-or-later unless otherwise noted below. */
// Vendored from the Backdrop catalog app, Apache-2.0.
//
//   https://github.com/Kyant0/AndroidLiquidGlass
//   commit b18eb0ff12c616546a68c72e7d0097f1ab286c87
//
// These are the library author's own example components rather than part of the
// published artifact, so there is nothing to depend on — they have to be copied.
// Kept as close to upstream as possible (package line and a few Material
// swaps aside) so a later upstream fix can be diffed in; see LICENSE-backdrop.txt.

package dev.nino.squareglass.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import dev.nino.squareglass.backdrop.RuntimeShader
import dev.nino.squareglass.backdrop.asComposeShader
import dev.nino.squareglass.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {

    private val pressProgressAnimationSpec =
        spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec =
        spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    private var pulseJob: Job? = null
    private var pressJob: Job? = null

    private fun animatePress(target: Float) {
        pulseJob?.cancel()
        pressJob?.cancel()
        pressJob = animationScope.launch {
            if (dev.nino.squareglass.SquareSettings.motion())
                pressProgressAnimation.animateTo(target, pressProgressAnimationSpec)
            else pressProgressAnimation.snapTo(0f)
        }
    }

    fun reset() {
        pulseJob?.cancel()
        pressJob?.cancel()
        pressJob = animationScope.launch { pressProgressAnimation.snapTo(0f) }
    }

    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    private val shader =
        if (isRuntimeShaderSupported()) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
            )
        } else {
            null
        }

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value.coerceIn(0f, 1f)
            // Additive white glints must stay below the dark navigation text's
            // contrast budget, including bright artwork and the selection film.
            val dark = dev.nino.squareglass.SquarePalette.isDark()
            val washAlpha = if (dark) .015f else .08f
            val lightAlpha = if (dark) .02f else .15f
            if (progress > 0f && dev.nino.squareglass.SquareSettings.motion()) {
                if (shader != null) {
                    drawRect(
                        Color.White.copy(washAlpha * progress),
                        blendMode = BlendMode.Plus
                    )
                    shader.apply {
                        val position = position(size, positionAnimation.value)
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform("color", Color.White.copy(lightAlpha * progress))
                        setFloatUniform("radius", size.minDimension * 1.5f)
                        setFloatUniform(
                            "position",
                            position.x.fastCoerceIn(0f, size.width),
                            position.y.fastCoerceIn(0f, size.height)
                        )
                    }
                    drawRect(
                        ShaderBrush(shader.asComposeShader()),
                        blendMode = BlendMode.Plus
                    )
                } else {
                    drawRect(
                        Color.White.copy((washAlpha + lightAlpha) * progress),
                        blendMode = BlendMode.Plus
                    )
                }
            }

            drawContent()
        }

    fun pulse(at: Offset = Offset.Zero) {
        pulseJob?.cancel()
        pressJob?.cancel()
        pulseJob = animationScope.launch {
            if (!dev.nino.squareglass.SquareSettings.motion()) return@launch
            startPosition = at
            positionAnimation.snapTo(at)
            pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec)
            pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec)
        }
    }

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                onDragStart = { down ->
                    startPosition = down.position
                    if(dev.nino.squareglass.SquareSettings.motion())animationScope.launch {
                        animatePress(1f)
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        animatePress(0f)
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        animatePress(0f)
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                }
            ) { change, _ ->
                animationScope.launch { positionAnimation.snapTo(change.position) }
            }
        }
}
