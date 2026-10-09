package bose.ankush.home.domain.remoteconfig

internal interface HomeRemoteConfigGate {
    fun initialize(onActivated: () -> Unit = {})

    fun isNotificationBannerEnabled(): Boolean
}
