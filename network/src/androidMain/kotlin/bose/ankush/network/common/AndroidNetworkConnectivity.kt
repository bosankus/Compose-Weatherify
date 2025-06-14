package bose.ankush.network.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Android implementation of NetworkConnectivity
 */
class AndroidNetworkConnectivity(
    private val context: Context
) : NetworkConnectivity {
    
    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}