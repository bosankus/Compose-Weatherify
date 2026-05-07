package bose.ankush.weatherify.base.common

/**
 * Platform-agnostic interface for device and app metadata.
 * Replaces direct Extension.* calls in shared/common code to enable KMP compatibility.
 *
 * Android provides an implementation backed by android.os.Build, BuildConfig, and Firebase.
 * iOS (or other KMP targets) would provide their own implementation.
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
