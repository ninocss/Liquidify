/* GPL-3.0-or-later. Geometry of the native output icon, including TalkBack clicks. */
package dev.nino.squareglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalView

object OutputAnchor {
    @JvmStatic @Composable fun watch(original: Modifier): Modifier {
        val view = LocalView.current
        DisposableEffect(view) { onDispose { SquareOutput.clearAnchor(view) } }
        return original.onGloballyPositioned {
            val bounds = it.boundsInWindow()
            val screen = IntArray(2); val window = IntArray(2)
            view.getLocationOnScreen(screen); view.getLocationInWindow(window)
            val dx = screen[0] - window[0]; val dy = screen[1] - window[1]
            SquareOutput.anchor(view, (bounds.left + dx).toInt(), (bounds.top + dy).toInt(),
                (bounds.right + dx).toInt(), (bounds.bottom + dy).toInt())
        }
    }
}
