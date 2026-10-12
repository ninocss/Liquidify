/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aspiro.wamp.mycollectionscreen.presentation.u as NativeCategory
import com.aspiro.wamp.mycollectionscreen.presentation.z as NativeCategoryAction
import kotlinx.coroutines.CoroutineScope

/** Layout adapter around native categories, purchases visibility and route handlers. */
object SquareLibrary {
    @JvmStatic
    @Composable
    fun Grid(items: List<NativeCategory>, scope: CoroutineScope, onAction: (Any) -> Any?, offline: Boolean, downloads: () -> Unit) {
        val configuration = LocalConfiguration.current
        val columns = if (configuration.fontScale > 1.3f) 1 else if (configuration.screenWidthDp >= 840) 4
            else if (configuration.screenWidthDp >= 600) 3 else 2
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var start = 0
            while (start < items.size + 1) {
                val rowStart = start
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var column = 0
                    while (column < columns) {
                        val index = rowStart + column
                        if (index < items.size) {
                            val item = items[index]
                            key(item.e) {
                                val callback = remember(scope, onAction, offline, item) {
                                    { NativeCategoryAction(scope, onAction, offline, item).invoke(); Unit }
                                }
                                Tile(item.a, item.b, item.c, !offline, item.e, callback, Modifier.weight(1f))
                            }
                        } else if (index == items.size) {
                            // The native Downloads callback is supplied by the same
                            // parent as the categories. It belongs to this grid,
                            // including the empty cell immediately after Artists.
                            Tile(0x7f080266, 0x7f14059a, 0x7f14059a, true, "Downloads", downloads, Modifier.weight(1f))
                        } else Spacer(Modifier.weight(1f))
                        column++
                    }
                }
                start += columns
            }
        }
    }

    @Composable
    private fun Tile(icon: Int, description: Int, label: Int, online: Boolean, tag: String, action: () -> Unit, modifier: Modifier) {
        // Offline entries still open TIDAL's explanation, so keep their label
        // and icon at readable contrast instead of dimming an active button.
        val ink = Color(if (online) SquarePalette.ink(NativeGlass.dark) else SquarePalette.secondary(NativeGlass.dark))
        val title = stringResource(label)
        val accessibility = stringResource(description)
        val currentAction = rememberUpdatedState(action)
        Column(
            SquareHome.shortcutSurface(modifier.sizeIn(minHeight = 96.dp).clip(RoundedCornerShape(20.dp)))
                .testTag(tag)
                .semantics(mergeDescendants = true) { if (accessibility != title) contentDescription = accessibility }
                // Offline categories remain actionable: the original callback
                // opens TIDAL's offline explanation rather than silently failing.
                .clickable(role = Role.Button, onClick = { currentAction.value() })
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(28.dp))
            Text(title, color = ink, fontFamily = SquareTypography.family, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    @JvmStatic
    @Composable
    fun Entry(icon: Int, description: Int, label: Int, online: Boolean, tag: String, onClick: () -> Unit) {
        val configuration = LocalConfiguration.current
        val columns = if (configuration.fontScale > 1.3f) 1 else if (configuration.screenWidthDp >= 840) 4
            else if (configuration.screenWidthDp >= 600) 3 else 2
        val action = rememberUpdatedState(onClick)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Tile(icon,description,label,online,tag,{action.value()},Modifier.weight(1f))
            repeat(columns-1) {Spacer(Modifier.weight(1f))}
        }
    }
}
