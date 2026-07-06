package bose.ankush.home.domain.remoteconfig

internal interface HomeRemoteConfigGate {
    fun initialize()

    fun isNotificationBannerEnabled(): Boolean
}
