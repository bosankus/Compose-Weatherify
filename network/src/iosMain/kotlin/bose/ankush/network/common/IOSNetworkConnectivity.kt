package bose.ankush.network.common

import platform.Foundation.NSFileManager
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityFlags
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.darwin.NULL

/**
 * iOS implementation of NetworkConnectivity
 */
class IOSNetworkConnectivity : NetworkConnectivity {
    
    override fun isNetworkAvailable(): Boolean {
        val reachability = SCNetworkReachabilityCreateWithName(
            NULL,
            "www.apple.com"
        ) ?: return false
        
        val flags = ULongArray(1)
        if (SCNetworkReachabilityGetFlags(reachability, flags)) {
            return (flags[0].toInt() and kSCNetworkReachabilityFlagsReachable.toInt()) != 0
        }
        
        return false
    }
}