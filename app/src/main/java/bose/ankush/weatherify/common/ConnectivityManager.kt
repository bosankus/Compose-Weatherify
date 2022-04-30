@file:Suppress("DEPRECATION")

package bose.ankush.weatherify.common

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object ConnectivityManager {

    @SuppressLint("ServiceCast")
    fun isNetworkAvailable(context: Context): Boolean {
        val isConnected: Boolean
        val connManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connManager.activeNetwork ?: return false
            val activeNetwork =
                connManager.getNetworkCapabilities(networkCapabilities) ?: return false
            when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            val networkInfo = connManager.activeNetworkInfo ?: return false
            networkInfo.isConnected
        }
        return isConnected
    }
}
