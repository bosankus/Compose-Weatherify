package bose.ankush.home.data.remoteconfig

import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfig
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigSettings
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal class FirebaseHomeRemoteConfigGate : HomeRemoteConfigGate {
    private val remoteConfig: FIRRemoteConfig? by lazy {
        try {
            FIRRemoteConfig.remoteConfig()
        } catch (_: Exception) {
            null
        }
    }

    override fun initialize() {
        val config = remoteConfig ?: return
        config.configSettings =
            FIRRemoteConfigSettings().apply {
                minimumFetchInterval = DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS
            }
        config.fetchAndActivateWithCompletionHandler { _, _ -> }
    }

    override fun isNotificationBannerEnabled(): Boolean =
        remoteConfig?.configValueForKey(ENABLE_NOTIFICATION_KEY)?.boolValue ?: false

    companion object {
        private const val DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS = 3600.0
        private const val ENABLE_NOTIFICATION_KEY = "enable_notification"
    }
}
