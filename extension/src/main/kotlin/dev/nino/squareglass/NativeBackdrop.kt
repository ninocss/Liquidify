/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.nino.squareglass.backdrop.Backdrop
import dev.nino.squareglass.backdrop.backdrops.emptyBackdrop
import dev.nino.squareglass.backdrop.backdrops.layerBackdrop
import dev.nino.squareglass.backdrop.backdrops.rememberLayerBackdrop

/** Connects the native fragment page to the separate native navigation ComposeView. */
object NativeBackdrop {
    private var nativeSource: NativeViewBackdrop? by mutableStateOf(null)
    private var playerSource: Backdrop? = null
    var active: Backdrop by mutableStateOf(emptyBackdrop())
        private set
    // The player is in another window. During its entry/exit the Activity's
    // accessories remain visible and must keep sampling their own page.
    val accessory: Backdrop get() = nativeSource ?: active

    fun attachNative(view: android.view.View) {
        if (nativeSource?.matches(view) == true) return
        nativeSource?.release()
        val source = NativeViewBackdrop(view)
        nativeSource = source
        active = playerSource ?: source
    }

    fun releaseNative(source: NativeViewBackdrop) {
        if (nativeSource === source) {
            nativeSource = null
            active = playerSource ?: emptyBackdrop()
        }
    }

    @JvmStatic fun transition() {nativeSource?.transition()}

    /** Record only the player background, excluding the glass controls above it. */
    @JvmStatic
    @Composable
    fun capturePlayer(original: Modifier): Modifier {
        DesignState.revision
        val backdrop = rememberLayerBackdrop()
        val view = LocalView.current
        // The full-screen native player owns a Dialog window, separate from MainActivity.
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val bars = window?.let { PlayerWindow.attach(it.decorView); SystemBarTheme.installPlayerWindow(it) }
            onDispose { bars?.release() }
        }
        DisposableEffect(backdrop) {
            playerSource = backdrop
            active = backdrop
            onDispose {
                if (playerSource === backdrop) {
                    playerSource = null
                    active = nativeSource ?: emptyBackdrop()
                    nativeSource?.requestCapture()
                }
            }
        }
        return if (SquareSettings.glass("player")) original.layerBackdrop(backdrop) else original
    }

    @JvmStatic
    @Composable
    fun capture(original: Modifier): Modifier {
        val draw: ContentDrawScope.() -> Unit = remember {
            var first = true
            {
                drawContent()
                if (first) {
                    first = false
                    Log.i("SquareGlass", "native page recorded for AndroidLiquidGlass")
                }
            }
        }
        val backdrop = rememberLayerBackdrop(onDraw = draw)
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, backdrop) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) nativeSource?.requestCapture()
                if (event == Lifecycle.Event.ON_RESUME && nativeSource == null && playerSource == null) active = backdrop
                if (event == Lifecycle.Event.ON_PAUSE && active === backdrop) active = emptyBackdrop()
            }
            lifecycle.addObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && nativeSource == null && playerSource == null) active = backdrop
            onDispose {
                lifecycle.removeObserver(observer)
                if (active === backdrop) active = emptyBackdrop()
            }
        }
        return original.layerBackdrop(backdrop)
    }
}
