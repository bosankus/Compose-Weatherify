package bose.ankush.home.presentation.home

internal sealed interface HomeEffect {
    data object RequestNotificationPermission : HomeEffect
}
