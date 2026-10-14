/* GPL-3.0-or-later. Native View bridge to AndroidLiquidGlass catalog spring motion. */
package dev.nino.squareglass

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.AndroidUiDispatcher
import dev.nino.squareglass.material.LiquidMotion
import kotlinx.coroutines.*
import java.util.WeakHashMap
import java.util.function.Consumer

object LiquidViewMotion {
    private class Binding : View.OnAttachStateChangeListener {
        val scope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)
        val jobs = HashMap<String, Job>()
        override fun onViewAttachedToWindow(view: View) {}
        override fun onViewDetachedFromWindow(view: View) {
            if (jobs.containsKey("press")) { view.scaleX = 1f; view.scaleY = 1f }
            scope.cancel()
            bindings.remove(view)
            view.removeOnAttachStateChangeListener(this)
        }
    }
    private val bindings = WeakHashMap<View, Binding>()

    @JvmStatic fun cancel(view: View, key: String) { bindings[view]?.jobs?.remove(key)?.cancel() }

    @JvmStatic fun animate(view: View, key: String, from: Float, target: Float,
        settle: Boolean, update: Consumer<Float>, finished: Runnable?) {
        cancel(view, key)
        if (!SquareSettings.motion()) { update.accept(target); finished?.run(); return }
        val binding = bindings.getOrPut(view) { Binding().also { view.addOnAttachStateChangeListener(it) } }
        val job = binding.scope.launch(start = CoroutineStart.LAZY) {
            val animation = Animatable(from)
            animation.animateTo(target, if (settle) LiquidMotion.settle() else LiquidMotion.shape()) {
                update.accept(if (SquareSettings.motion()) value else target)
            }
            update.accept(target)
            finished?.run()
        }
        binding.jobs[key] = job
        job.invokeOnCompletion { if (binding.jobs[key] === job) binding.jobs.remove(key) }
        update.accept(from)
        job.start()
    }

    @JvmStatic fun press(view: View, pressed: Boolean) {
        val target = if (pressed && SquareSettings.motion()) .97f else 1f
        animate(view, "press", view.scaleX, target, false, Consumer { scale ->
            view.scaleX = scale
            // The catalog stretches one axis while compressing the other.
            view.scaleY = 1f + (1f - scale) * .5f
        }, null)
    }
}
