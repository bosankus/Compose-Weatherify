package bose.ankush.home.data.remoteconfig

import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

internal class FirebaseHomeRemoteConfigGate : HomeRemoteConfigGate {
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    override fun initialize(onActivated: () -> Unit) {
        val configSettings =
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS
            }
        remoteConfig.setConfigSettingsAsync(configSettings)
        // In-app default is off. A missing or failed read also stays off.
        remoteConfig.setDefaultsAsync(mapOf(ENABLE_LEAVE_BY_FAKE_DOOR_KEY to false))
        remoteConfig.fetchAndActivate().addOnCompleteListener { onActivated() }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun isNotificationBannerEnabled(): Boolean =
        try {
            remoteConfig.getBoolean(ENABLE_NOTIFICATION_KEY)
        } catch (_: Exception) {
            false
        }

    @Suppress("TooGenericExceptionCaught")
    override fun isLeaveByFakeDoorEnabled(): Boolean =
        try {
            remoteConfig.getBoolean(ENABLE_LEAVE_BY_FAKE_DOOR_KEY)
        } catch (_: Exception) {
            false
        }

    companion object {
        private const val DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
        private const val ENABLE_NOTIFICATION_KEY = "enable_notification"
        private const val ENABLE_LEAVE_BY_FAKE_DOOR_KEY = "enable_leave_by_fake_door"
    }
}
