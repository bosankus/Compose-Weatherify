package bose.ankush.weatherify.base.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

object Extension {
    fun Double.toCelsius() = (this - KELVIN_OFFSET).roundToInt().toString()

    fun String.getIconUrl(size: String = "@2x.png") = "$WEATHER_IMG_URL$this$size"

    fun String.formatTextCapitalization() = replaceFirstChar { it.uppercaseChar() }

    fun isDeviceSDKAndroid13OrAbove() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun Context.openAppSystemSettings() =
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            },
        )

    fun Context.openLocationSettings() =
        startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        )

    @SuppressLint("QueryPermissionsNeeded")
    fun Context.openAppLocaleSettings() {
        val resolved = resolveLocaleIntent() ?: return
        startActivity(resolved)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun Context.resolveLocaleIntent(): Intent? {
        val pm = packageManager
        val candidates =
            buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(
                        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                }
                add(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
                add(
                    Intent(Settings.ACTION_LOCALE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
        return candidates.firstOrNull { intent ->
            try {
                intent.resolveActivity(pm) != null
            } catch (_: Exception) {
                false
            }
        }
    }

    fun Context.hasLocationPermission(): Boolean =
        listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ).all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

    fun Context.hasNotificationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            ACCESS_NOTIFICATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun String.wrapText(): String {
        val words = this.split(" ")
        return if (words.size == 2) {
            "${words[0]}\n${words[1]}"
        } else {
            this
        }
    }
}
