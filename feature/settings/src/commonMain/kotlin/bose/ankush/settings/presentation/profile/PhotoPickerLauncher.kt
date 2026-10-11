package bose.ankush.settings.presentation.profile

import androidx.compose.runtime.Composable
import bose.ankush.settings.domain.ProfilePhoto

/** Opens the system photo picker. */
internal fun interface PhotoPickerLauncher {
    fun launch()
}

/**
 * The platform photo picker, or null where picking is not supported yet (iOS), in which case
 * the screen hides the photo controls. [onPicked] receives the photo already re-encoded for
 * upload; [onUnreadable] fires when the chosen image cannot be decoded. Cancelling the picker
 * calls neither.
 */
@Composable
internal expect fun rememberProfilePhotoPicker(
    onPicked: (ProfilePhoto) -> Unit,
    onUnreadable: () -> Unit,
): PhotoPickerLauncher?
