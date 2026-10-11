package bose.ankush.commonui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * The dark look shared by the home screen's companions (profile/settings, language): a dark
 * backdrop, the home screen's soft off-white ink (not pure white), and translucent rounded cards.
 * These screens draw it themselves, whatever the app's light/dark theme is.
 */

/** Same ink as the home screen's ContentOnDark. */
val NightInk = Color(0xFFE7E4DC)
val NightInkMuted = NightInk.copy(alpha = 0.62f)
val NightInkFaint = NightInk.copy(alpha = 0.12f)
val NightDanger = Color(0xFFFF8A80)

val NightCardFill = Color.White.copy(alpha = 0.06f)
val NightCardStroke = Color.White.copy(alpha = 0.08f)

/** Dialogs, sheets, and indicators on top of the backdrop. */
val NightSurface = Color(0xFF1B2028)

val NightBackdrop =
    Brush.verticalGradient(
        colors = listOf(Color(0xFF1C2430), Color(0xFF12161C), Color(0xFF0B0E12)),
    )

val NightCardShape = RoundedCornerShape(20.dp)
val NightPillShape = RoundedCornerShape(percent = 50)

/** Light status-bar icons while composed, for a dark backdrop. Restored on leave. No-op on iOS. */
@Composable
expect fun LightSystemBarIcons()
