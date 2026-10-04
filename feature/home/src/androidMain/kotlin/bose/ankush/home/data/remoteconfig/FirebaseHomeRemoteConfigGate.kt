package bose.ankush.home.data.remoteconfig

import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import timber.log.Timber

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
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            Timber.tag(LEAVE_BY_FAKE_DOOR_LOG_TAG).d(
                "fetchAndActivate finished success=%s exception=%s key=%s value=%s",
                task.isSuccessful,
                task.exception?.message,
                ENABLE_LEAVE_BY_FAKE_DOOR_KEY,
                remoteConfig.getBoolean(ENABLE_LEAVE_BY_FAKE_DOOR_KEY),
            )
            onActivated()
        }
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
            val enabled = remoteConfig.getBoolean(ENABLE_LEAVE_BY_FAKE_DOOR_KEY)
            Timber.tag(LEAVE_BY_FAKE_DOOR_LOG_TAG).d(
                "isLeaveByFakeDoorEnabled key=%s value=%s",
                ENABLE_LEAVE_BY_FAKE_DOOR_KEY,
                enabled,
            )
            enabled
        } catch (e: Exception) {
            Timber.tag(LEAVE_BY_FAKE_DOOR_LOG_TAG).d(
                "isLeaveByFakeDoorEnabled key=%s failed=%s value=false",
                ENABLE_LEAVE_BY_FAKE_DOOR_KEY,
                e.message,
            )
            false
        }

    companion object {
        private const val DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
        private const val ENABLE_NOTIFICATION_KEY = "enable_notification"
        private const val ENABLE_LEAVE_BY_FAKE_DOOR_KEY = "enable_leave_by_fake_door"
        private const val LEAVE_BY_FAKE_DOOR_LOG_TAG = "LeaveByFakeDoor"
    }
}
