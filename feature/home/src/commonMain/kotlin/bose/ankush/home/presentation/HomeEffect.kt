package bose.ankush.home.presentation

internal sealed interface HomeEffect {
    data object RequestNotificationPermission : HomeEffect

    data object RequestLocationPermission : HomeEffect

    data object RequestGpsPermission : HomeEffect

    /** Notifications were declined for good; only the OS notification settings can turn them on. */
    data object OpenNotificationSettings : HomeEffect
}
