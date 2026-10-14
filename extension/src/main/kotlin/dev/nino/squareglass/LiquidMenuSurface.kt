/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import android.app.Activity
import android.graphics.RenderNode
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.nino.squareglass.backdrop.Backdrop
import dev.nino.squareglass.backdrop.drawBackdrop
import dev.nino.squareglass.backdrop.effects.blur
import dev.nino.squareglass.backdrop.effects.lens
import dev.nino.squareglass.backdrop.effects.vibrancy
import dev.nino.squareglass.backdrop.highlight.Highlight
import dev.nino.squareglass.backdrop.shadow.Shadow

/** AndroidLiquidGlass paint behind native controls, including separate popup windows. */
class LiquidMenuSurface(activity: Activity, snapshot: PlayerWindow.Snapshot?) {
    private val source = snapshot?.view(activity, false) ?: PlayerWindow.source(activity)
    private val ownerSource = activity.window.decorView
    private val node = RenderNode("SquareGlass menu backdrop")
    private val sourceScreen = IntArray(2)
    private val surfaces = ArrayList<ComposeView>()

    init {
        if (snapshot != null) {
            source.layout(0, 0, snapshot.width, snapshot.height)
            sourceScreen[0] = snapshot.x; sourceScreen[1] = snapshot.y
        }
        // One capture per presentation. The menu is in another window and is never
        // part of its own backdrop. No ongoing page capture is needed at rest.
        if (SquareSettings.preset() != 2 && source.width > 0 && source.height > 0) {
            if (snapshot == null) source.getLocationOnScreen(sourceScreen)
            node.setPosition(0, 0, source.width, source.height)
            val canvas = node.beginRecording(source.width, source.height)
            try { source.draw(canvas) } finally { node.endRecording() }
        }
    }

    @JvmOverloads fun paint(container: FrameLayout, radius: Float, emphasized: Boolean, sheet: Boolean = false) {
        // Compose creates its window recomposer at PopupDecorView, above our
        // content. PopupWindow does not install tree owners like a Dialog does.
        // The parent attaches before its Compose children, so supply owners there.
        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                val root = v.rootView
                if (root.findViewTreeLifecycleOwner() == null)
                    root.setViewTreeLifecycleOwner(ownerSource.findViewTreeLifecycleOwner())
                if (root.findViewTreeSavedStateRegistryOwner() == null)
                    root.setViewTreeSavedStateRegistryOwner(ownerSource.findViewTreeSavedStateRegistryOwner())
            }
            override fun onViewDetachedFromWindow(v: View) {}
        })
        val view = ComposeView(container.context)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setViewTreeLifecycleOwner(ownerSource.findViewTreeLifecycleOwner())
        view.setViewTreeSavedStateRegistryOwner(ownerSource.findViewTreeSavedStateRegistryOwner())
        (view as View).isClickable = false
        (view as View).isFocusable = false
        (view as View).importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        var backdrop by mutableStateOf<Backdrop>(WindowBackdrop(view))
        val lastPosition = intArrayOf(Int.MIN_VALUE, Int.MIN_VALUE)
        val position = IntArray(2)
        val trackPosition = ViewTreeObserver.OnPreDrawListener {
            // Native BottomSheetBehavior translates a parent outside Compose.
            // Refresh only when that screen position changes; otherwise the pane
            // keeps its captured layer and schedules no animation or capture loop.
            (view as View).getLocationOnScreen(position)
            if (position[0] != lastPosition[0] || position[1] != lastPosition[1]) {
                lastPosition[0] = position[0]; lastPosition[1] = position[1]
                backdrop = WindowBackdrop(view)
            }
            true
        }
        (view as View).addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { v.viewTreeObserver.addOnPreDrawListener(trackPosition) }
            override fun onViewDetachedFromWindow(v: View) {
                if (v.viewTreeObserver.isAlive) v.viewTreeObserver.removeOnPreDrawListener(trackPosition)
            }
        })
        view.setContent { Pane(backdrop, radius, emphasized, sheet) }
        container.addView(view, 0, FrameLayout.LayoutParams(-1, -1))
        val outline = GradientDrawable().apply {
            setColor(android.graphics.Color.TRANSPARENT)
            val r = radius * container.resources.displayMetrics.density
            cornerRadii = if (sheet) floatArrayOf(r,r,r,r,0f,0f,0f,0f) else FloatArray(8) { r }
        }
        container.background = outline
        container.clipToOutline = true
        surfaces.add(view)
    }

    fun clearWithin(container: ViewGroup) {
        val iterator = surfaces.iterator()
        while (iterator.hasNext()) {
            val view = iterator.next()
            var parent = (view as View).parent
            while (parent != null && parent !== container) parent = parent.parent
            if (parent === container) {
                view.disposeComposition()
                iterator.remove()
            }
        }
    }

    fun release() {
        for (view in surfaces) view.disposeComposition()
        surfaces.clear()
        node.discardDisplayList()
    }

    private inner class WindowBackdrop(private val target: View) : Backdrop {
        override val isCoordinatesDependent = true
        private val screen = IntArray(2)

        override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
            if (coordinates == null || !coordinates.isAttached || !node.hasDisplayList()) return
            target.getLocationOnScreen(screen)
            // Popup and bottom-sheet window coordinates do not share the Activity
            // origin. Each ComposeView contains one full-size pane at local (0,0),
            // so its native screen position is the exact origin to sample from.
            translate((sourceScreen[0] - screen[0]).toFloat(), (sourceScreen[1] - screen[1]).toFloat()) {
                drawIntoCanvas { canvas ->
                    if (canvas.nativeCanvas.isHardwareAccelerated) canvas.nativeCanvas.drawRenderNode(node)
                }
            }
        }
    }

    @Composable
    private fun Pane(backdrop: Backdrop, radius: Float, emphasized: Boolean, sheet: Boolean) {
        DesignState.revision
        val dark = NativeGlass.dark
        val shape = if (sheet) RoundedCornerShape(topStart = radius.dp, topEnd = radius.dp)
            else RoundedCornerShape(radius.dp)
        val film = if (dark) Color(0xFF33343A) else Color.White
        val opacity = SquareSettings.readableOpacity(dark)
        val modifier = if (SquareSettings.preset() == 2) {
            Modifier.clip(shape).background(Color(SquarePalette.surface(dark)))
        } else Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(SquareSettings.blur().dp.toPx())
                if (SquareSettings.preset() == 0) {
                    val reach = minOf(size.width, size.height) * .35f
                    lens((60 * SquareSettings.refraction()).dp.toPx().coerceAtMost(reach),
                        (60 * SquareSettings.refraction()).dp.toPx().coerceAtMost(reach))
                }
            },
            highlight = { Highlight.Default },
            shadow = { Shadow(radius = (if (emphasized) 4 else 12).dp) },
            onDrawSurface = {
                ProbeDiagnostics.glassDraws++
                drawRect(film.copy(alpha = opacity))
                if (emphasized) drawRect(Color(SquarePalette.ink(dark)).copy(alpha = .06f))
            },
        )
        Box(Modifier.fillMaxSize().then(modifier))
    }
}
