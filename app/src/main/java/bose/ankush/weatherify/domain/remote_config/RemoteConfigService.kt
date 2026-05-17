package bose.ankush.weatherify.domain.remote_config

interface RemoteConfigService {
    fun initialize()

    fun getBoolean(
        key: String,
        defaultValue: Boolean = false,
    ): Boolean
}
