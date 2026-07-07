package bose.ankush.home.presentation

internal sealed interface HomeEffect {
    data object RequestNotificationPermission : HomeEffect
    data object RequestLocationPermission : HomeEffect
    data object RequestGpsPermission : HomeEffect
    data object OpenSettings: HomeEffect
}
