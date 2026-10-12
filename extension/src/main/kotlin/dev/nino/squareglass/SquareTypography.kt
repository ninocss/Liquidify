/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

object SquareTypography {
    // Existing APK font IDs, replaced by the fingerprinted Typography module.
    val family = FontFamily(
        Font(0x7f090005, FontWeight.Normal),
        Font(0x7f090004, FontWeight.Medium),
        Font(0x7f090003, FontWeight.SemiBold),
        Font(0x7f090002, FontWeight.Bold),
    )
}
