package bose.ankush.home.presentation.home

internal sealed interface HomeIntent {
    data object FetchLocation : HomeIntent

    data object Refresh : HomeIntent

    data object ResetLocationOverride : HomeIntent

    data object EnableNotificationBanner : HomeIntent

    data object DismissNotificationBanner : HomeIntent

    data class UpdateNotificationPermissionState(
        val hasPermission: Boolean,
    ) : HomeIntent

    data class NotificationPermissionResult(
        val isGranted: Boolean,
        val isPermanentlyDeclined: Boolean,
    ) : HomeIntent
}
