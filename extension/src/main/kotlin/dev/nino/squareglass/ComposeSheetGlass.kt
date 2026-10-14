/* GPL-3.0-or-later. Cross-window glass for native Compose modal sheets. */
package dev.nino.squareglass

import android.app.Activity
import android.graphics.RenderNode
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.nino.squareglass.backdrop.Backdrop
import dev.nino.squareglass.backdrop.drawBackdrop
import dev.nino.squareglass.backdrop.effects.blur
import dev.nino.squareglass.backdrop.effects.lens
import dev.nino.squareglass.backdrop.effects.vibrancy
import dev.nino.squareglass.backdrop.highlight.Highlight

object ComposeSheetGlass {
    @JvmStatic @Composable fun surface(original: Modifier, shape: Shape): Modifier {
        DesignState.revision
        if (SquareSettings.themeEnabled() == 0) return original
        val activity = SheetChrome.activity(LocalContext.current) ?: return original
        val target = LocalView.current
        val sample = remember(target, activity) { Sample(activity, target) }
        DisposableEffect(sample) { onDispose { sample.release() } }
        val dark = NativeGlass.dark
        if (SquareSettings.preset() == 2) return original.clip(shape).background(Color(SquarePalette.surface(dark)))
        val film = (if (dark) Color(0xFF33343A) else Color.White).copy(alpha = SquareSettings.readableOpacity(dark))
        return original.drawBackdrop(sample, shape = { shape }, effects = {
            vibrancy(); blur(SquareSettings.blur().dp.toPx())
            // Preserve the host outline. Arbitrary path shapes can still use
            // blur; the rounded-rectangle shader cannot describe their edges.
            if (SquareSettings.preset() == 0 && (shape is CornerBasedShape || shape === RectangleShape)) {
                val reach = minOf(size.width, size.height) * .35f
                val depth = (60 * SquareSettings.refraction()).dp.toPx().coerceAtMost(reach)
                lens(depth, depth)
            }
        }, highlight = { if (shape === RectangleShape) null else Highlight.Default }, onDrawSurface = {
            drawRect(film)
        })
    }

    private class Sample(activity: Activity, private val target: View) : Backdrop {
        private val snapshot = PlayerWindow.take()
        private val node = RenderNode("Liquidify Compose sheet source")
        private val origin = IntArray(2)
        private val screen = IntArray(2)
        override val isCoordinatesDependent = true
        init {
            // PlayerWindow can already point at this very fullscreen dialog.
            // Recording that decor would make the sheet sample itself and form
            // a RenderNode cycle. A player-menu snapshot is safe; otherwise use
            // the Activity window underneath the native modal window.
            val source = snapshot?.view(activity, false) ?: activity.window.decorView
            if (snapshot != null) {
                source.layout(0, 0, snapshot.width, snapshot.height)
                origin[0] = snapshot.x; origin[1] = snapshot.y
            } else source.getLocationOnScreen(origin)
            if (source.rootView !== target.rootView && source.width > 0 && source.height > 0) {
                node.setPosition(0, 0, source.width, source.height)
                val canvas = node.beginRecording(source.width, source.height)
                try { source.draw(canvas) } finally { node.endRecording() }
            }
        }
        override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
            if (coordinates == null || !coordinates.isAttached || !node.hasDisplayList()) return
            target.rootView.getLocationOnScreen(screen)
            val offset = coordinates.positionInWindow()
            translate(origin[0] - screen[0] - offset.x, origin[1] - screen[1] - offset.y) {
                drawIntoCanvas { if (it.nativeCanvas.isHardwareAccelerated) it.nativeCanvas.drawRenderNode(node) }
            }
        }
        fun release() { node.discardDisplayList(); snapshot?.release() }
    }
}
