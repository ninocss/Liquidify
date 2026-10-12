/* GPL-3.0-or-later. Animate the thumb, never the authoritative slider value. */
package dev.nino.squareglass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

object SliderMotion {
    @JvmStatic @Composable fun nativeScale(target: Float): State<Float> {
        DesignState.revision
        val motion=SquareSettings.motion()
        return animateFloatAsState(if(motion)target else 1f,
            if(motion)spring(dampingRatio=.85f,stiffness=700f) else snap(),label="slider thumb")
    }
    @Composable fun thumb(original: Modifier,source: MutableInteractionSource): Modifier {
        val pressed by source.collectIsPressedAsState()
        val dragged by source.collectIsDraggedAsState()
        val scale by nativeScale(if(pressed || dragged)1.2f else 1f)
        return original.graphicsLayer {scaleX=scale;scaleY=scale}
    }
}
