package bose.ankush.settings.presentation.component

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import bose.ankush.commonui.theme.NightBackdrop
import bose.ankush.commonui.theme.NightCardFill
import bose.ankush.commonui.theme.NightCardShape
import bose.ankush.commonui.theme.NightCardStroke
import bose.ankush.commonui.theme.NightDanger
import bose.ankush.commonui.theme.NightInk
import bose.ankush.commonui.theme.NightInkFaint
import bose.ankush.commonui.theme.NightInkMuted
import bose.ankush.commonui.theme.NightPillShape
import bose.ankush.commonui.theme.NightSurface

// The profile screen's look: the shared night surface from common-ui, under local names.

internal val SettingsInk = NightInk
internal val SettingsInkMuted = NightInkMuted
internal val SettingsInkFaint = NightInkFaint
internal val SettingsDanger = NightDanger
internal val SettingsCardFill = NightCardFill
internal val SettingsCardStroke = NightCardStroke
internal val SettingsSurface = NightSurface
internal val SettingsBackdrop = NightBackdrop
internal val SettingsCardShape = NightCardShape
internal val SettingsPillShape = NightPillShape

/**
 * Card fill under the current backdrop. The night fill on the plain backdrop; as a home page the
 * cards sit on the weather photo, so they take the darker fill iOS uses there to stay readable.
 */
internal val LocalSettingsCardFill = staticCompositionLocalOf { NightCardFill }
internal val SettingsPhotoCardFill = Color.Black.copy(alpha = 0.38f)

/** Lifts loose type (email, section titles) off a bright photo; unseen on the night backdrop. */
internal val SettingsTextShadow =
    Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 6f)
