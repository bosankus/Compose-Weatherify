package bose.ankush.home.data.remoteconfig

import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.bose.ankush.feature.home.FIRRemoteConfig
import swiftPMImport.bose.ankush.feature.home.FIRRemoteConfigSettings

@OptIn(ExperimentalForeignApi::class)
internal class FirebaseHomeRemoteConfigGate : HomeRemoteConfigGate {
    private val remoteConfig: FIRRemoteConfig? by lazy {
        try {
            FIRRemoteConfig.remoteConfig()
        } catch (_: Exception) {
            null
        }
    }

    override fun initialize(onActivated: () -> Unit) {
        val config = remoteConfig
        if (config == null) {
            onActivated()
            return
        }
        config.configSettings =
            FIRRemoteConfigSettings().apply {
                minimumFetchInterval = DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS
            }
        config.fetchAndActivateWithCompletionHandler { _, _ -> onActivated() }
    }

    override fun isNotificationBannerEnabled(): Boolean =
        remoteConfig?.configValueForKey(ENABLE_NOTIFICATION_KEY)?.boolValue ?: false

    companion object {
        private const val DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS = 3600.0
        private const val ENABLE_NOTIFICATION_KEY = "enable_notification"
    }
}
