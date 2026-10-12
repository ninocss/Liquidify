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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs


data class FloatBounds(val start: Float, val endInclusive: Float)

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: FloatBounds,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDragCancelled: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {

    private val valueAnimationSpec =
        spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec =
        spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec =
        spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec =
        spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec =
        spring(0.7f, 250f, 0.001f)

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation =
        Animatable(0f, 5f)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()

    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val progress: Float get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = if(dev.nino.squareglass.SquareSettings.motion())pressProgressAnimation.value else 0f
    val scaleX: Float get() = if(dev.nino.squareglass.SquareSettings.motion())scaleXAnimation.value else initialScale
    val scaleY: Float get() = if(dev.nino.squareglass.SquareSettings.motion())scaleYAnimation.value else initialScale
    val velocity: Float get() = if(dev.nino.squareglass.SquareSettings.motion())velocityAnimation.value else 0f

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                onDragStarted(down.position)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                // A sibling can consume a tap or Android can cancel a pointer.
                // Cancellation must never commit another native navigation action.
                onDragCancelled()
                release()
            }
        ) { change, dragAmount ->
            onDrag(size, dragAmount)
        }
    }

    fun press() {
        if(!dev.nino.squareglass.SquareSettings.motion())return
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        if(!dev.nino.squareglass.SquareSettings.motion())return
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
        animationScope.launch {
            if(dev.nino.squareglass.SquareSettings.motion())
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() } }
            else valueAnimation.snapTo(targetValue)
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                if(!dev.nino.squareglass.SquareSettings.motion()) {
                    valueAnimation.snapTo(value.coerceIn(valueRange.start,valueRange.endInclusive))
                    pressProgressAnimation.snapTo(0f);scaleXAnimation.snapTo(initialScale);scaleYAnimation.snapTo(initialScale)
                    velocityAnimation.snapTo(0f)
                    return@mutate
                }
                press()
                val targetValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            android.os.SystemClock.uptimeMillis(),
            Offset(value, 0f)
        )
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
