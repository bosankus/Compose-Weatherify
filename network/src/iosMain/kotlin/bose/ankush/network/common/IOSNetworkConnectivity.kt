package bose.ankush.network.common

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityFlagsVar
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable

@Suppress("unused")
class IOSNetworkConnectivity : NetworkConnectivity {
    @OptIn(ExperimentalForeignApi::class)
    override fun isNetworkAvailable(): Boolean {
        val reachability =
            SCNetworkReachabilityCreateWithName(
                null,
                "www.apple.com",
            ) ?: return false

        return memScoped {
            val flags = alloc<SCNetworkReachabilityFlagsVar>()
            if (SCNetworkReachabilityGetFlags(reachability, flags.ptr)) {
                (flags.value and kSCNetworkReachabilityFlagsReachable) != 0u &&
                        (flags.value and kSCNetworkReachabilityFlagsConnectionRequired) == 0u
            } else {
                false
            }
        }
    }
}
