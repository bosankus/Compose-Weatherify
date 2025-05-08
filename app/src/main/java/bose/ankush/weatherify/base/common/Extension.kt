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

    fun Double.toCelsius() = (this - 273).roundToInt().toString()

    fun String.getIconUrl(size: String = "@2x.png") = "$WEATHER_IMG_URL$this$size"

    fun String.formatTextCapitalization() = replaceFirstChar { it.uppercaseChar() }

    fun isDeviceSDKAndroid13OrAbove() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun Context.openAppSystemSettings() = startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun Context.openAppLocaleSettings() = startActivity(
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    )

    fun Context.hasLocationPermission(): Boolean = listOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ).all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
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

    fun String.wrapText(): String {
        val words: List<String> = this.split(" ")
        return if (words.size == 2) {
            "${words[0]}\n${words[1]}"
        } else {
            this
        }
    }

    fun Context.callNumber(): Boolean = hasPhoneCallPermission().also { hasPermission ->
        if (hasPermission) startActivity(
            Intent(Intent.ACTION_CALL).apply {
                data = PHONE_NUMBER.toUri()
            }
        )
    }
}
