/* Square Glass for TIDAL - integration probe. GPL-3.0-or-later. */
package dev.nino.squareglass

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.Text
import dev.nino.squareglass.backdrop.backdrops.layerBackdrop
import dev.nino.squareglass.backdrop.backdrops.rememberLayerBackdrop
import dev.nino.squareglass.backdrop.drawBackdrop
import dev.nino.squareglass.backdrop.effects.blur
import dev.nino.squareglass.backdrop.effects.lens

/**
 * First integration gate: retain TIDAL content and its action callback, record
 * that content, then draw a real AndroidLiquidGlass lens over it.
 * This is NOT the finished redesign, and is never packaged as a release.
 */
object GlassProbe {
    @JvmStatic
    fun wrapNative(activity: Activity, content: @Composable () -> Unit): @Composable () -> Unit {
        // Resource ID verified against the pinned APK. This excludes appScaffold.
        val page = activity.findViewById<android.view.View>(0x7f0b02e6)
        NativeBackdrop.attachNative(page)
        ProbeDiagnostics.install(activity, page)
        return {
        @Suppress("DEPRECATION")
        Wrap(onAction = { ProbeDiagnostics.nativeBackActions++; activity.onBackPressed() }, content = content)
        }
    }

    @JvmStatic
    @Composable
    fun Wrap(onAction: () -> Unit, content: @Composable () -> Unit) {
        val backdrop = rememberLayerBackdrop()
        val shape = remember { RoundedCornerShape(28.dp) }
        SideEffect { Log.i("SquareGlass", "probe composed with native TIDAL content") }
        // MainActivity's ComposeView is an accessory overlay, not the full page.
        // Its intrinsic height must remain controlled by the original content.
        Box {
            Box(Modifier.layerBackdrop(backdrop)) { content() }
            Box(Modifier.matchParentSize()) {
                Box(
                Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                    .size(56.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = { blur(4.dp.toPx()); lens(18.dp.toPx(), 10.dp.toPx()) },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.14f)) },
                    )
                    .semantics { contentDescription = "SquareGlass probe: native back" }
                    .clickable(onClick = {
                        Log.i("SquareGlass", "probe forwards native Activity back action")
                        onAction()
                    })
                ) { Text("←", color = Color.White, modifier = Modifier.align(Alignment.Center)) }
            }
        }
    }
}
