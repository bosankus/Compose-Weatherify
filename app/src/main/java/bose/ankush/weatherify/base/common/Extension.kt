package bose.ankush.weatherify.base.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlin.math.roundToInt

/**Created by
Author: Ankush Bose
Date: 06,May,2021
 **/

object Extension {

    fun Double.toCelsius(): String = (this - 273).roundToInt().toString()

    fun Context.openAppSystemSettings() {
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        })
    }

    fun Context.hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun Context.hasPhoneCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            ACCESS_PHONE_CALL
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun Context.hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            ACCESS_NOTIFICATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun String.getIconUrl(size: String = "@2x.png"): String {
        return "$WEATHER_IMG_URL$this$size"
    }

    fun String.wrapText(): String {
        val words: List<String> = this.split(" ")
        return if (words.size == 2) {
            "${words[0]}\n${words[1]}"
        } else {
            this
        }
    }

    fun String.formatTextCapitalization(): String {
        val firstLetter = this[0].uppercaseChar()
        val restOfString = this.substring(1)
        return firstLetter + restOfString
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun Context.openAppLocaleSettings() {
        startActivity(Intent().apply {
            action = Settings.ACTION_APP_LOCALE_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        })
    }

    fun Context.callNumber(): Boolean {
        return if (this.hasPhoneCallPermission()) {
            startActivity(Intent().apply {
                action = Intent.ACTION_CALL
                data = PHONE_NUMBER.toUri()
            })
            true
        } else {
            false
        }
    }

    fun isDeviceSDKAndroid13OrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
