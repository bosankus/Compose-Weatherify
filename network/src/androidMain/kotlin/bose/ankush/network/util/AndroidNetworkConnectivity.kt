package bose.ankush.network.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Online means the active network can actually reach the internet, not just that Wi-Fi or mobile
 * data is switched on. Android probes each network it joins and only marks it
 * [NetworkCapabilities.NET_CAPABILITY_VALIDATED] once that probe gets through, so a Wi-Fi with no
 * uplink, or a captive portal not yet signed into, counts as offline.
 *
 * Requires ACCESS_NETWORK_STATE permission in AndroidManifest.xml:
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 */
class AndroidNetworkConnectivity(
    private val context: Context,
) : NetworkConnectivity {
    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
