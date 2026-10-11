package bose.ankush.settings.presentation.profile

import androidx.compose.runtime.Composable
import bose.ankush.settings.domain.ProfilePhoto

/** Not on iOS yet: needs a PHPickerViewController bridge. The screen hides the photo controls. */
@Composable
internal actual fun rememberProfilePhotoPicker(
    onPicked: (ProfilePhoto) -> Unit,
    onUnreadable: () -> Unit,
): PhotoPickerLauncher? = null
