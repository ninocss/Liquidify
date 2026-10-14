/* GPL-3.0-or-later. Draw-only spring feedback for the legacy favorite glyph. */
package dev.nino.squareglass

import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageView
import java.util.WeakHashMap
import java.util.function.Consumer
import java.util.function.IntConsumer
import kotlin.math.sin

object NativeIconGlass {
    private class State(var selected: Boolean) {
        var progress = 1f
        var token: Any? = null
        var x = 0f
        var y = 0f
        var moved = false
        var tap = -20000L
    }
    private val states = WeakHashMap<View, State>()

    private fun pulse(view: View, state: State) {
        if (!SquareSettings.motion()) return
        LiquidViewMotion.animate(view, "icon", 0f, 1f, false, Consumer {
            state.progress = it
            view.invalidate()
        }, null)
    }

    @JvmStatic fun attach(view: View) {
        if (view.id != 0x7f0b02c6 || states.containsKey(view)) return
        val state = State(view.isSelected)
        states[view] = state
        state.token = IconMotion.registerNative(view, IntConsumer { pulse(view, state) })
    }

    @JvmStatic fun detach(view: View) {
        val state = states.remove(view) ?: return
        state.token?.let { IconMotion.unregisterNative(it) }
        LiquidViewMotion.cancel(view, "icon")
        view.invalidate()
    }

    @JvmStatic fun touch(view: View, event: MotionEvent) {
        val state = states[view] ?: return
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                state.x = event.x; state.y = event.y; state.moved = false
                pulse(view, state)
            }
            MotionEvent.ACTION_MOVE -> if (kotlin.math.hypot(event.x-state.x, event.y-state.y) >
                ViewConfiguration.get(view.context).scaledTouchSlop) state.moved = true
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> state.moved = true
            MotionEvent.ACTION_UP -> if (!state.moved) state.tap = SystemClock.uptimeMillis()
        }
    }

    @JvmStatic fun before(view: View, child: View, canvas: Canvas): Int {
        val state = states[view] ?: return -1
        if (child !is ImageView) return -1
        if (view.isSelected != state.selected) {
            state.selected = view.isSelected
            if (SystemClock.uptimeMillis()-state.tap < 10000 && SquareSettings.motion()) {
                pulse(view, state)
                if (state.selected) ProbeDiagnostics.likeAnimations++
            }
            state.tap = -20000L
        }
        if (!SquareSettings.motion() || state.progress >= 1f) return -1
        val scale = 1f + .06f * sin(Math.PI.toFloat() * state.progress.coerceIn(0f, 1f))
        val save = canvas.save()
        canvas.scale(scale, scale, child.left + child.width / 2f, child.top + child.height / 2f)
        return save
    }
}
