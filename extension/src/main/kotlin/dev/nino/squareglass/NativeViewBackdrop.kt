/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import android.graphics.RenderNode
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.hardware.HardwareBuffer
import android.media.ImageReader
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
    private var transitionPending = false
    private var notifyNextCapture = true
    private var ready = false
    private var frozen: Bitmap?=null
    private var frozenTop=0
    var handoverVersion by mutableIntStateOf(0)
        private set
    private val resume=Runnable {source.invalidate()}
    val hasSample: Boolean get() = ready || frozen != null
    override val isCoordinatesDependent = true

    fun transition() {
        // RenderNode recordings reference their child View render nodes. Removing
        // a tab empties those children even when this recording is retained.
        // Resolve the last stable sample into independent GPU pixels BEFORE the
        // native action; never redraw a transitioning Fragment to take a copy.
        if(frozen==null && node.hasDisplayList() && source.isAttachedToWindow) freeze()
        // Give the native transaction one traversal to install its animations.
        // Subsequent retries follow actual transition state, never a timer.
        transitionPending = true
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

    private fun freeze() {
        val width=source.width
        val height=minOf(source.height,(300*source.resources.displayMetrics.density).toInt())
        if(width<=0 || height<=0)return
        val crop=RenderNode("SquareGlass stable accessory sample")
        val renderer=HardwareRenderer()
        var reader: ImageReader?=null
        try {
            frozenTop=source.height-height
            crop.setPosition(0,0,width,height)
            val canvas=crop.beginRecording(width,height)
            try {canvas.translate(0f,-frozenTop.toFloat());canvas.drawRenderNode(node)} finally {crop.endRecording()}
            reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
            renderer.setSurface(reader.surface)
            renderer.setContentRoot(crop)
            renderer.setOpaque(false)
            renderer.start()
            ProbeDiagnostics.accessoryRenderStatus=renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
            val image=reader.acquireLatestImage()
            if(image!=null)try {
                val buffer=image.hardwareBuffer
                if(buffer!=null)try {
                    val wrapped=Bitmap.wrapHardwareBuffer(buffer,ColorSpace.get(ColorSpace.Named.SRGB))
                    // An ImageReader buffer belongs to its producer queue. Resolve
                    // it once before closing that queue; no child render node or
                    // reusable producer buffer remains in the transition sample.
                    frozen=wrapped?.copy(Bitmap.Config.ARGB_8888,false)
                    wrapped?.recycle()
                    frozen?.let {ProbeDiagnostics.accessorySampleAlpha=android.graphics.Color.alpha(it.getPixel(width/2,height/2))}
                } finally {buffer.close()}
            } finally {image.close()}
            if(frozen!=null) {ProbeDiagnostics.accessoryFreezes++;handoverVersion++}
        } catch (_: RuntimeException) {
            // A device that cannot allocate the offscreen surface keeps its live
            // recording and native transitions; it never blocks navigation.
            if(frozen!=null) {frozen=null;handoverVersion++}
        } finally {renderer.destroy();reader?.close();crop.discardDisplayList()}
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
            if (ready || frozen != null) {
                ready = false
                frozen = null
                handoverVersion++
            }
            return true
        }
        // View.draw also advances legacy Fragment/Animation drawing state. Replaying
        // the container during an exit can finish a disappearing child before the
        // real window draw, then expose it again on the next frame. Keep the last
        // glass sample until the native transition owns its final frame.
        if (NativeBackdrop.active === this && source.isShown && source.width > 0 && source.height > 0) {
            if (transitionPending) {
                transitionPending = false
                retryNextFrame()
                return true
            }
            if (transitioning(source)) {
                retryNextFrame()
                return true
            }
            source.removeCallbacks(resume)
            node.setPosition(0, 0, source.width, source.height)
            val canvas = node.beginRecording(source.width, source.height)
            try { source.draw(canvas) } finally { node.endRecording() }
            source.getLocationInWindow(location)
            val changed = !ready || frozen != null || notifyNextCapture
            ready = true
            frozen = null
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

    private fun transitioning(view: View): Boolean {
        if (view.visibility != View.VISIBLE) return false
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
        // Only a frozen/live handover invalidates sampling surfaces. Per-capture
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
                    val sample=frozen
                    if(sample!=null)canvas.nativeCanvas.drawBitmap(sample,0f,frozenTop.toFloat(),null)
                    else canvas.nativeCanvas.drawRenderNode(node)
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
        frozen=null
        ready=false
    }

    override fun onViewAttachedToWindow(v: View) {
        source.viewTreeObserver.removeOnPreDrawListener(this)
        source.viewTreeObserver.addOnPreDrawListener(this)
        requestCapture()
    }
    override fun onViewDetachedFromWindow(v: View) {
        if(source.viewTreeObserver.isAlive) source.viewTreeObserver.removeOnPreDrawListener(this)
        source.removeCallbacks(resume)
        // A transient detach during tab replacement must retain the sample.
    }
}
