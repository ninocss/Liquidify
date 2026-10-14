/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import android.graphics.RenderNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import dev.nino.squareglass.backdrop.Backdrop
import dev.nino.squareglass.backdrop.internal.InverseLayerScope

/**
 * Captures only the native fragment container, which is a sibling of navigation.
 * Recording the whole Activity would include the glass itself and create a cycle.
 * This bridge also covers native XML pages, not just Wave/Compose screens.
 */
class NativeViewBackdrop(private val source: View) : Backdrop, ViewTreeObserver.OnPreDrawListener,
    View.OnAttachStateChangeListener {
    private val node = RenderNode("SquareGlass native TIDAL page")
    private val location = IntArray(2)
    private val inverse = InverseLayerScope()
    private val capturePolicy = BackdropCapturePolicy()
    private var notifyNextCapture = true
    private var ready = false
    var handoverVersion by mutableIntStateOf(0)
        private set
    private val resume=Runnable {source.invalidate()}
    val hasSample: Boolean get() = ready
    override val isCoordinatesDependent = true

    fun transition() {
        // Navigation must not synchronously render/read GPU pixels on the UI
        // thread. Leave the native window in charge of every transition frame.
        capturePolicy.transition()
        requestCapture()
    }

    fun requestCapture() {
        notifyNextCapture = true
        source.removeCallbacks(resume)
        source.postOnAnimation(resume)
    }

    private fun retryNextFrame() {
        source.removeCallbacks(resume)
        source.postOnAnimation(resume)
    }

    init {
        source.viewTreeObserver.addOnPreDrawListener(this)
        source.addOnAttachStateChangeListener(this)
        requestCapture()
    }

    override fun onPreDraw(): Boolean {
        ProbeDiagnostics.nativeDrawTraversals++
        if (!SquareSettings.glass("nav") && !SquareSettings.glass("mini")) {
            if (node.hasDisplayList()) node.discardDisplayList()
            if (ready) {
                ready = false
                handoverVersion++
            }
            return true
        }
        // View.draw also advances legacy Fragment/Animation drawing state. Replaying
        // the container during an exit can finish a disappearing child before the
        // real window draw, then expose it again on the next frame. Keep the last
        // glass sample until the native transition owns its final frame.
        if (NativeBackdrop.active === this && source.isShown && source.width > 0 && source.height > 0) {
            if (!capturePolicy.allowCapture(transitioning(source))) {
                retryNextFrame()
                return true
            }
            source.removeCallbacks(resume)
            node.setPosition(0, 0, source.width, source.height)
            val canvas = node.beginRecording(source.width, source.height)
            try { source.draw(canvas) } finally { node.endRecording() }
            source.getLocationInWindow(location)
            val changed = !ready || notifyNextCapture
            ready = true
            notifyNextCapture = false
            if (changed) {
                handoverVersion++
                ProbeDiagnostics.accessoryHandovers++
                if (ProbeDiagnostics.accessoryFirstCaptureUptime == 0L) {
                    ProbeDiagnostics.accessoryFirstCaptureUptime = android.os.SystemClock.uptimeMillis()
                    ProbeDiagnostics.accessoryFirstCaptureTraversal = ProbeDiagnostics.nativeDrawTraversals
                }
            }
            ProbeDiagnostics.nativeCaptures++
        }
        return true
    }

    private companion object {
        // These are app-owned AndroidX fields from the pinned host, not hidden
        // Android platform APIs. Resolve once, outside the frame traversal.
        val fragmentTransitionFields = listOf("transitioningFragmentViews", "disappearingFragmentChildren").mapNotNull { name ->
            try { androidx.fragment.app.FragmentContainerView::class.java.getDeclaredField(name).apply { isAccessible = true } }
            catch (_: ReflectiveOperationException) { null }
        }
    }

    private fun transitioning(view: View): Boolean {
        // FragmentContainerView retains disappearing children outside childCount.
        // Check its transition lists even if its current children are hidden.
        if (view is androidx.fragment.app.FragmentContainerView && fragmentTransitionFields.any {
                (it.get(view) as? Collection<*>)?.isNotEmpty() == true
            }) return true
        if (view.visibility != View.VISIBLE) return false
        if (view.hasTransientState()) return true
        val animation=view.animation
        // Indefinitely animated content (for example a loading spinner) is not
        // a page transition and must not prevent the first backdrop recording.
        if(animation!=null && animation.repeatCount != android.view.animation.Animation.INFINITE && !animation.hasEnded())return true
        if(view is ViewGroup) {
            if (view.layoutTransition?.isRunning == true) return true
            // The Activity page root may permanently contain multiple visible
            // siblings. Only a FragmentContainerView owns fragment replacement.
            if(view is androidx.fragment.app.FragmentContainerView) {
                val group = view as ViewGroup
                var visible=0
                for(i in 0 until group.childCount)if(group.getChildAt(i).visibility==View.VISIBLE)visible++
                if(visible>1)return true
            }
            for(i in 0 until view.childCount)if(transitioning(view.getChildAt(i)))return true
        }
        return false
    }

    override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
        // Only a stable-page handover invalidates sampling surfaces. Per-capture
        // snapshot writes here would form a perpetual capture/redraw feedback loop.
        handoverVersion
        if (coordinates == null || !coordinates.isAttached || !node.hasDisplayList()) return
        if(source.isAttachedToWindow)source.getLocationInWindow(location)
        val target = coordinates.positionInWindow()
        withTransform({
            if (layerBlock != null) {
                inverse.reset()
                with(inverse) { inverseTransform(density, layerBlock) }
            }
            translate(location[0] - target.x, location[1] - target.y)
        }) {
            drawIntoCanvas { canvas ->
                if (canvas.nativeCanvas.isHardwareAccelerated) {
                    canvas.nativeCanvas.drawRenderNode(node)
                }
            }
        }
    }

    fun matches(view: View): Boolean = source === view

    fun release() {
        source.removeCallbacks(resume)
        if (source.viewTreeObserver.isAlive) source.viewTreeObserver.removeOnPreDrawListener(this)
        source.removeOnAttachStateChangeListener(this)
        node.discardDisplayList()
        ready=false
    }

    override fun onViewAttachedToWindow(v: View) {
        source.viewTreeObserver.removeOnPreDrawListener(this)
        source.viewTreeObserver.addOnPreDrawListener(this)
        capturePolicy.transition()
        requestCapture()
    }
    override fun onViewDetachedFromWindow(v: View) {
        if(source.viewTreeObserver.isAlive) source.viewTreeObserver.removeOnPreDrawListener(this)
        source.removeCallbacks(resume)
        // A transient detach during tab replacement must retain the sample.
    }
}
