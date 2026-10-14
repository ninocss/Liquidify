/* GPL-3.0-or-later. AndroidLiquidGlass paint behind the legacy favorite glyph. */
package dev.nino.squareglass

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dev.nino.squareglass.material.InteractiveHighlight
import java.util.WeakHashMap
import java.util.function.IntConsumer

object NativeIconGlass {
    private class State(val paint: ComposeView, selected: Boolean) {
        var selected = selected
        var event by mutableIntStateOf(0)
        var token: Any? = null
        var x = 0f
        var y = 0f
        var moved = false
        var tap = -20000L
    }
    private val states = WeakHashMap<View, State>()

    @JvmStatic fun attach(view: View) {
        if (view.id != 0x7f0b02c6 || view !is ViewGroup || states.containsKey(view)) return
        val paint = ComposeView(view.context)
        paint.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        (paint as View).importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        (paint as View).isClickable = false
        (paint as View).isFocusable = false
        val state = State(paint, view.isSelected)
        states[view] = state
        state.token = IconMotion.registerNative(view, IntConsumer { if (SquareSettings.motion()) state.event++ })
        paint.setContent {
            val scope = rememberCoroutineScope()
            val highlight = remember(scope) { InteractiveHighlight(scope) }
            DesignState.revision
            val motion = SquareSettings.motion()
            val event = state.event
            LaunchedEffect(event, motion) {
                if (event > 0 && motion) highlight.pulse(Offset((paint as View).width / 2f, (paint as View).height / 2f))
                else highlight.reset()
            }
            Box(GlassMotion.feedback(Modifier.fillMaxSize(), highlight))
        }
        view.addView(paint, 0, ViewGroup.LayoutParams(-1, -1))
    }
    @JvmStatic fun detach(view: View) {
        val state = states.remove(view) ?: return
        state.token?.let { IconMotion.unregisterNative(it) }
        state.paint.disposeComposition()
        (view as? ViewGroup)?.removeView(state.paint)
    }
    @JvmStatic fun touch(view: View, event: MotionEvent) {
        val state = states[view] ?: return
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                state.x = event.x; state.y = event.y; state.moved = false
                if (SquareSettings.motion()) state.event++
            }
            MotionEvent.ACTION_MOVE -> if (kotlin.math.hypot(event.x-state.x, event.y-state.y) >
                ViewConfiguration.get(view.context).scaledTouchSlop) state.moved = true
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> state.moved = true
            MotionEvent.ACTION_UP -> if (!state.moved) state.tap = SystemClock.uptimeMillis()
        }
    }
    @JvmStatic fun observe(view: View, child: View) {
        val state = states[view] ?: return
        if (child !is ImageView || view.isSelected == state.selected) return
        state.selected = view.isSelected
        if (SystemClock.uptimeMillis()-state.tap < 10000 && SquareSettings.motion()) {
            state.event++
            if (state.selected) ProbeDiagnostics.likeAnimations++
        }
        state.tap = -20000L
    }
}
