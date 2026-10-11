package bose.ankush.settings.presentation.profile

import bose.ankush.settings.domain.ProfilePhoto

internal data class ProfileState(
    val email: String? = null,
    val photoUrl: String? = null,
    val photoOperation: PhotoOperation = PhotoOperation.Idle,
    val isRemoveConfirmVisible: Boolean = false,
    /** The change-or-remove sheet, opened from the avatar once there is a photo. */
    val isPhotoOptionsVisible: Boolean = false,
    /** A user pull-to-refresh is running. The automatic refresh on open never sets this. */
    val isRefreshing: Boolean = false,
) {
    val isBusy: Boolean get() = photoOperation != PhotoOperation.Idle
    val canRemovePhoto: Boolean get() = photoUrl != null && !isBusy
}

internal enum class PhotoOperation {
    Idle,
    Uploading,
    Removing,
}

internal sealed interface ProfileIntent {
    /** Quiet refetch, when the screen opens. */
    data object Refresh : ProfileIntent

    /** User pulled to refresh: same refetch, with the indicator showing until it settles. */
    data object PullToRefresh : ProfileIntent

    data class PhotoPicked(
        val photo: ProfilePhoto,
    ) : ProfileIntent

    /** The picked image could not be read or re-encoded on the device. */
    data object PhotoUnreadable : ProfileIntent

    data object OpenPhotoOptions : ProfileIntent

    data object DismissPhotoOptions : ProfileIntent

    data object RequestRemovePhoto : ProfileIntent

    data object DismissRemovePhoto : ProfileIntent

    data object ConfirmRemovePhoto : ProfileIntent
}

/**
 * One-off messages. The screen turns each into localized text. A failure carries the server's
 * own reason when it gave one (for example a size or format rejection), shown under it.
 */
internal sealed interface ProfileMessage {
    data object PhotoUpdated : ProfileMessage

    data object PhotoRemoved : ProfileMessage

    data object PhotoUnreadable : ProfileMessage

    data class UploadFailed(
        val reason: String? = null,
    ) : ProfileMessage

    data class RemoveFailed(
        val reason: String? = null,
    ) : ProfileMessage
}
