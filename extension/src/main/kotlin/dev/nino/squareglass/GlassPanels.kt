/* GPL-3.0-or-later. Page-local material without capturing its own controls. */
package dev.nino.squareglass

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.nino.squareglass.backdrop.Backdrop
import dev.nino.squareglass.backdrop.drawBackdrop
import dev.nino.squareglass.backdrop.effects.lens
import dev.nino.squareglass.backdrop.highlight.Highlight
import dev.nino.squareglass.backdrop.shadow.Shadow

object GlassPanels {
    @JvmStatic fun field(original: Modifier): Modifier = panel(original,28f)

    fun panel(original: Modifier,radius: Float,emphasized: Boolean = false): Modifier {
        if(SquareSettings.themeEnabled()==0) return original
        val dark=SquarePalette.isDark()
        val shape=RoundedCornerShape(radius.dp)
        if(SquareSettings.preset()==2) return original.clip(shape).background(Color(SquarePalette.surface(dark)))
        // These panels live inside the captured native page. Sampling that page
        // would create a RenderNode cycle. Its flat base is the local backdrop.
        val base=Color(SquarePalette.base(dark))
        val source=object : Backdrop {
            override val isCoordinatesDependent=false
            override fun DrawScope.drawBackdrop(density: Density,coordinates: LayoutCoordinates?,layerBlock: (GraphicsLayerScope.()->Unit)?) { drawRect(base) }
        }
        return original.drawBackdrop(source,shape={shape},effects={
            if(SquareSettings.preset()==0) {
                val reach=minOf(size.width,size.height)*.2f
                lens((12*SquareSettings.refraction()).dp.toPx().coerceAtMost(reach),(18*SquareSettings.refraction()).dp.toPx().coerceAtMost(reach))
            }
        },highlight={Highlight.Default},shadow={Shadow(radius=6.dp)},onDrawSurface={
            drawRect(Color.White.copy(alpha=if(dark) .08f else .52f))
            if(emphasized) drawRect(Color(SquarePalette.ink(dark)).copy(alpha=.10f))
        })
    }
}
