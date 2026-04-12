package bose.ankush.weatherify.base.common

/** Android implementation of [DeviceInfoProvider] backed by the existing Extension helpers. */
class AndroidDeviceInfoProvider : DeviceInfoProvider {
    override fun getDeviceModel(): String = Extension.getDeviceModel()
    override fun getOperatingSystem(): String = Extension.getOperatingSystem()
    override fun getOsVersion(): String = Extension.getOsVersion()
    override fun getAppVersion(): String = Extension.getAppVersion()
    override fun getRegistrationSource(): String = Extension.getRegistrationSource()
    override fun getIpAddress(): String? = Extension.getIpAddress()
    override fun getCurrentUtcTimestamp(): String = Extension.getCurrentUtcTimestamp()
    override suspend fun getFirebaseToken(): String? = Extension.getFirebaseToken()
}
