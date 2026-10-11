package bose.ankush.settings.presentation.profile

import androidx.compose.runtime.Composable
import bose.ankush.settings.generated.resources.Res
import bose.ankush.settings.generated.resources.profile_photo_read_failed_txt
import bose.ankush.settings.generated.resources.profile_photo_remove_failed_txt
import bose.ankush.settings.generated.resources.profile_photo_removed_txt
import bose.ankush.settings.generated.resources.profile_photo_updated_txt
import bose.ankush.settings.generated.resources.profile_photo_upload_failed_txt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private fun ProfileMessage.text(): StringResource =
    when (this) {
        ProfileMessage.PhotoUpdated -> Res.string.profile_photo_updated_txt
        ProfileMessage.PhotoRemoved -> Res.string.profile_photo_removed_txt
        ProfileMessage.PhotoUnreadable -> Res.string.profile_photo_read_failed_txt
        is ProfileMessage.UploadFailed -> Res.string.profile_photo_upload_failed_txt
        is ProfileMessage.RemoveFailed -> Res.string.profile_photo_remove_failed_txt
    }

private fun ProfileMessage.reason(): String? =
    when (this) {
        is ProfileMessage.UploadFailed -> reason
        is ProfileMessage.RemoveFailed -> reason
        else -> null
    }

internal fun ProfileMessage.isError(): Boolean =
    this is ProfileMessage.UploadFailed || this is ProfileMessage.RemoveFailed || this == ProfileMessage.PhotoUnreadable

/** The localized line, then the server's reason under it when there is one. */
@Composable
internal fun ProfileMessage.fullText(): String {
    val text = stringResource(text())
    return reason()?.let { "$text\n$it" } ?: text
}
