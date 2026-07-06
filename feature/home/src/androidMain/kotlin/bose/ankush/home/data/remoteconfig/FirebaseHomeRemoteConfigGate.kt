package bose.ankush.home.data.remoteconfig

import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

internal class FirebaseHomeRemoteConfigGate : HomeRemoteConfigGate {
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    override fun initialize() {
        val configSettings =
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS
            }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun isNotificationBannerEnabled(): Boolean =
        try {
            remoteConfig.getBoolean(ENABLE_NOTIFICATION_KEY)
        } catch (_: Exception) {
            false
        }

    companion object {
        private const val DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
        private const val ENABLE_NOTIFICATION_KEY = "enable_notification"
    }
}
