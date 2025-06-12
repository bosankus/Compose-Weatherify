package bose.ankush.weatherify.data.remote_config

import bose.ankush.weatherify.R
import bose.ankush.weatherify.domain.remote_config.RemoteConfigService
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of the RemoteConfigService interface.
 * This class handles all interactions with Firebase Remote Config.
 */
@Singleton
class FirebaseRemoteConfigService @Inject constructor() : RemoteConfigService {

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
    private val tag = "${FirebaseRemoteConfigService::class.simpleName} ->"

    /**
     * Initializes the Firebase Remote Config with default settings.
     * Sets default values from the XML resource file.
     */
    override fun initialize() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS
        }

        remoteConfig.apply {
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(R.xml.remote_config_defaults)
        }

        Timber.tag(tag).d("Firebase Remote Config initialized")
    }

    /**
     * Gets a boolean value from Firebase Remote Config.
     * @param key The key for the configuration value
     * @param defaultValue The default value to return if the key is not found
     * @return The boolean value from Firebase Remote Config, or the default value if not found
     */
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            remoteConfig.getBoolean(key)
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error getting boolean value for key: $key")
            defaultValue
        }
    }

    companion object {
        private const val DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS = 3600L // 1 hour
    }
}
