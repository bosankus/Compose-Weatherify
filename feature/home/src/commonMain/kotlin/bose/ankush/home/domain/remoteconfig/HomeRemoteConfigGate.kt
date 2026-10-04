package bose.ankush.home.domain.remoteconfig

internal interface HomeRemoteConfigGate {
    fun initialize(onActivated: () -> Unit = {})

    fun isNotificationBannerEnabled(): Boolean

    /**
     * Leave-by fake door. Default off. iOS always returns false: the card is Android-only.
     */
    fun isLeaveByFakeDoorEnabled(): Boolean
}
