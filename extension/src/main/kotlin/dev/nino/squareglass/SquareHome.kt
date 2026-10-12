/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Home presentation only; the server's filters, labels and callbacks remain native. */
object SquareHome {
    @JvmStatic
    @Composable
    fun actionsEnabled(): Boolean {
        DesignState.revision
        return SquareSettings.integer("homeActions", 0, 0, 1) == 1
    }
    private fun activity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> activity(context.baseContext)
        else -> null
    }

    @JvmStatic
    @Composable
    fun Actions() {
        DesignState.revision
        val context = LocalContext.current
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("Autoplay") { NativePlayback.autoplay(context, false) }
            Action("Rediscover") { NativePlayback.autoplay(context, true) }
            Action("Shuffle history") { NativePlayback.shuffleHistory(context) }
        }
    }

    @Composable
    private fun Action(label: String, onClick: () -> Unit) {
        val dark = NativeGlass.dark
        Box(Modifier.sizeIn(minHeight = 48.dp).clip(CircleShape)
            .background(Color(SquarePalette.surface(dark)))
            .border(1.dp, Color(SquarePalette.ink(dark)).copy(alpha = .12f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center) {
            Text(label, color = Color(SquarePalette.ink(dark)), fontFamily = SquareTypography.family,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }

    @JvmStatic
    @Composable
    fun Filter(label: String, selected: Boolean, onClick: () -> Unit) {
        val dark = NativeGlass.dark
        val ink = Color(SquarePalette.ink(dark))
        val base = Color(SquarePalette.base(dark))
        val surface = Color(SquarePalette.surface(dark))
        val duration = if (SquareSettings.motion()) 220 else 0
        val fill by animateColorAsState(if (selected) ink else surface, tween(duration), label = "homeFilterFill")
        val text by animateColorAsState(if (selected) base else ink, tween(duration), label = "homeFilterInk")
        val action = rememberUpdatedState(onClick)
        Box(
            Modifier.sizeIn(minHeight = 48.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, ink.copy(alpha = if (selected) 0f else .10f), CircleShape)
                .selectable(selected, role = Role.RadioButton, onClick = { if (!selected) action.value() })
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = text, fontFamily = SquareTypography.family, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }

    @JvmStatic
    fun shortcutSurface(original: Modifier): Modifier {
        val dark = SquarePalette.isDark()
        val shape = RoundedCornerShape(20.dp)
        // Inline cards are inside the captured native page. A neutral film here
        // avoids feeding the page's own backdrop capture back into itself.
        val film = if (dark) Color(0xFF24272E) else Color.White.copy(alpha = .84f)
        val edge = if (dark) Color.White.copy(alpha = .09f) else Color.Black.copy(alpha = .055f)
        return original.background(film, shape).border(1.dp, edge, shape)
    }

    @JvmStatic
    fun toolbarButton(original: Modifier): Modifier {
        val dark = SquarePalette.isDark()
        val film = if (dark) Color(0xFF24272E) else Color.White.copy(alpha = .84f)
        val edge = if (dark) Color.White.copy(alpha = .12f) else Color.White
        return original.padding(end = 16.dp).sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape).background(film).border(1.dp, edge, CircleShape).padding(10.dp)
    }
}
