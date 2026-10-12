/* GPL-3.0-or-later. Intent first; the host owns the full-screen transition. */
package dev.nino.squareglass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

object NativeMiniGestures {
    @JvmStatic fun showSkip(): Boolean = !SquareSettings.flag("miniGestures",true)
    @JvmStatic @Composable
    fun decorate(original: Modifier,openPlayer: ()->Unit): Modifier {
        DesignState.revision
        if(!SquareSettings.flag("miniGestures",true)) return original
        val open by rememberUpdatedState(openPlayer)
        val density=LocalDensity.current
        val openDistance=with(density) { 28.dp.toPx() }
        val skipDistance=with(density) { 48.dp.toPx() }
        val flickSpeed=with(density) { 600.dp.toPx() }
        // Moving/scaling the pill outside its native clipped accessory slot was
        // both delayed and visibly cut off. Keep it in its measured bounds and
        // hand an upward drag to the native player as soon as intent is clear.
        return original.semantics {
            customActions=listOf(
                CustomAccessibilityAction("Open player") { open(); true },
                CustomAccessibilityAction("Next track") { NativePlayback.next(false); true },
                CustomAccessibilityAction("Previous track") { NativePlayback.next(true); true })
        }.pointerInput(openDistance,skipDistance) {
            awaitEachGesture {
                val first=awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial)
                val velocity=VelocityTracker(); velocity.addPosition(first.uptimeMillis,first.position)
                var dx=0f; var dy=0f; var axis=0; var opened=false; var cancelled=false
                try {
                    while(true) {
                        val event=awaitPointerEvent(PointerEventPass.Initial)
                        if(event.changes.size!=1) { cancelled=true; break }
                        val change=event.changes.firstOrNull { it.id==first.id } ?: break
                        velocity.addPosition(change.uptimeMillis,change.position)
                        dx=change.position.x-first.position.x; dy=change.position.y-first.position.y
                        if(axis==0) {
                            val slop=viewConfiguration.touchSlop
                            if(abs(dx)>slop && abs(dx)>abs(dy)*1.15f) axis=1
                            else if(-dy>slop && -dy>abs(dx)*1.05f) axis=2
                            else if(dy>slop && dy>abs(dx)) { cancelled=true; break }
                        }
                        if(axis!=0) change.consume()
                        if(axis==2 && !opened && -dy>=openDistance) { opened=true; open() }
                        if(!change.pressed) break
                    }
                    if(!cancelled && !opened) {
                        val speed=velocity.calculateVelocity()
                        if(axis==2 && -dy>viewConfiguration.touchSlop && (-dy>=openDistance || -speed.y>=flickSpeed)) open()
                        else if(axis==1 && abs(dx)>viewConfiguration.touchSlop &&
                            (abs(dx)>=skipDistance || abs(speed.x)>=flickSpeed)) NativePlayback.next(dx>0)
                    }
                } finally { /* No deferred animation, view transform or retained gesture state. */ }
            }
        }
    }
}
