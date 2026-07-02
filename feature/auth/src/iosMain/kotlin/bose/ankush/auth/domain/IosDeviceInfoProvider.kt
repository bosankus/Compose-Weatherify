package bose.ankush.auth.domain

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_ntop
import platform.posix.AF_INET
import platform.posix.INET_ADDRSTRLEN
import platform.posix.sockaddr_in

internal class IosDeviceInfoProvider : DeviceInfoProvider {
    override fun getDeviceModel(): String = UIDevice.currentDevice.model

    override fun getOperatingSystem(): String = OPERATING_SYSTEM

    override fun getOsVersion(): String = UIDevice.currentDevice.systemVersion

    override fun getAppVersion(): String =
        (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String) ?: ""

    override fun getRegistrationSource(): String = REGISTRATION_SOURCE

    override fun getIpAddress(): String? = readLocalIpAddress()

    override fun getCurrentUtcTimestamp(): String = currentUtcTimestamp()

    override suspend fun getFirebaseToken(): String? = null

    private companion object {
        const val OPERATING_SYSTEM = "iOS"
        const val REGISTRATION_SOURCE = "iOS App"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readLocalIpAddress(): String? =
    runCatching {
        memScoped {
            val listPtr = alloc<CPointerVar<ifaddrs>>()
            if (getifaddrs(listPtr.ptr) != 0) return@memScoped null

            try {
                var current = listPtr.value
                var result: String? = null
                while (current != null && result == null) {
                    val entry = current.pointed
                    val socketAddress = entry.ifa_addr
                    val interfaceName = entry.ifa_name?.toKString()
                    if (socketAddress != null &&
                        socketAddress.pointed.sa_family.toInt() == AF_INET &&
                        interfaceName != "lo0"
                    ) {
                        val addressIn = socketAddress.reinterpret<sockaddr_in>().pointed
                        val buffer = ByteArray(INET_ADDRSTRLEN)
                        result =
                            buffer.usePinned { pinned ->
                                val address = pinned.addressOf(0)
                                inet_ntop(
                                    AF_INET,
                                    addressIn.sin_addr.ptr,
                                    address,
                                    INET_ADDRSTRLEN.toUInt(),
                                )
                                address.toKString()
                            }
                    }
                    current = entry.ifa_next
                }
                result
            } finally {
                freeifaddrs(listPtr.value)
            }
        }
    }.getOrNull()
