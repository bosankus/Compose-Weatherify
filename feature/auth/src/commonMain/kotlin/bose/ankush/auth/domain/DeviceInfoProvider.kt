package bose.ankush.auth.domain

/**
 * Platform-agnostic interface for device and app metadata used during registration.
 * Android provides [bose.ankush.weatherify.base.common.AndroidDeviceInfoProvider].
 */
interface DeviceInfoProvider {
    fun getDeviceModel(): String

    fun getOperatingSystem(): String

    fun getOsVersion(): String

    fun getAppVersion(): String

    fun getRegistrationSource(): String

    fun getIpAddress(): String?

    fun getCurrentUtcTimestamp(): String

    suspend fun getFirebaseToken(): String?
}
