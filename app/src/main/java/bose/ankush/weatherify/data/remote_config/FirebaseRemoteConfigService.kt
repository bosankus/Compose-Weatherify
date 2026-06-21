package bose.ankush.weatherify.data.remote_config

import bose.ankush.weatherify.R
import bose.ankush.weatherify.domain.remote_config.RemoteConfigService
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteConfigService
@Inject
constructor() : RemoteConfigService {
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
    private val tag = "${FirebaseRemoteConfigService::class.simpleName} ->"

    override fun initialize() {
        val configSettings =
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS
            }

        remoteConfig.apply {
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(R.xml.remote_config_defaults)
        }

        Timber.tag(tag).d("Firebase Remote Config initialized")
    }

    @Suppress("TooGenericExceptionCaught")
    override fun getBoolean(
        key: String,
        defaultValue: Boolean,
    ): Boolean =
        try {
            remoteConfig.getBoolean(key)
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error getting boolean value for key: $key")
            defaultValue
        }

    companion object {
        private const val DEFAULT_MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
    }
}
