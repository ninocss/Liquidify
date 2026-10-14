/* GPL-3.0-or-later. Shared motion parameters from AndroidLiquidGlass's catalog. */
package dev.nino.squareglass.material

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

object LiquidMotion {
    // DampedDragAnimation's scale springs and InteractiveHighlight's release spring.
    fun <T> shape(): SpringSpec<T> = spring(dampingRatio = .7f, stiffness = 250f)
    fun <T> highlight(): SpringSpec<T> = spring(dampingRatio = .5f, stiffness = 300f)
    fun <T> settle(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1000f)
}
