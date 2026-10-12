/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object SquareShapes {
    private val cover = RoundedCornerShape(10.dp)
    private val playerCover = RoundedCornerShape(0.dp)
    private val shortcut = RoundedCornerShape(20.dp)
    @JvmStatic fun artwork(original: Float): RoundedCornerShape = cover
    @JvmStatic fun playerArtwork(original: Float): RoundedCornerShape = playerCover
    @JvmStatic fun shortcut(original: Float): RoundedCornerShape = shortcut
}
