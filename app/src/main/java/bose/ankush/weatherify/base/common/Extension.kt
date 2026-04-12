package bose.ankush.weatherify.base.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import bose.ankush.weatherify.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
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

    fun Context.openLocationSettings() = startActivity(
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    )

    @SuppressLint("QueryPermissionsNeeded")
    fun Context.openAppLocaleSettings() {
        // Try opening the per-app language settings if available, otherwise fall back safely
        val pm = packageManager
        // Primary: Per-app language settings (Android 13+)
        val appLocaleIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_LOCALE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        try {
            val canHandleAppLocale = appLocaleIntent.resolveActivity(pm) != null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && canHandleAppLocale) {
                startActivity(appLocaleIntent)
                return
            }
        } catch (_: Exception) {
            // Ignore and try fallbacks
        }

        // Fallback 1: App details/settings screen
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val canHandleAppDetails = appDetailsIntent.resolveActivity(pm) != null
            if (canHandleAppDetails) {
                startActivity(appDetailsIntent)
                return
            }
        } catch (_: Exception) {
            // Ignore and try next fallback
        }

        // Fallback 2: System language settings
        val localeSettingsIntent = Intent(Settings.ACTION_LOCALE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val canHandleLocaleSettings = localeSettingsIntent.resolveActivity(pm) != null
            if (canHandleLocaleSettings) {
                startActivity(localeSettingsIntent)
                return
            }
        } catch (_: Exception) {
            // Final fallback: do nothing; avoid crash
        }
    }

    fun Context.hasLocationPermission(): Boolean = listOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ).all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
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

    /**
     * Gets the device model (e.g., "Pixel 7 Pro", "iPhone 15")
     * @return The device model name
     */
    fun getDeviceModel(): String {
        return Build.MODEL
    }

    /**
     * Gets the operating system name (e.g., "Android")
     * @return The operating system name
     */
    fun getOperatingSystem(): String {
        return "Android"
    }

    /**
     * Gets the operating system version (e.g., "14", "13.1")
     * @return The operating system version
     */
    fun getOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    /**
     * Gets the app version from BuildConfig
     * @return The app version
     */
    fun getAppVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    /**
     * Gets the current UTC timestamp in ISO 8601 format
     * @return The current UTC timestamp
     */
    fun getCurrentUtcTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }

    /**
     * Gets the registration source
     * @return The registration source (e.g., "Android App")
     */
    fun getRegistrationSource(): String {
        return "Android App"
    }

    /**
     * Attempts to get the device's IP address
     * Note: This is a best-effort approach and may not always return the correct IP
     * @return The IP address or null if not available
     */
    fun getIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && !inetAddress.isLinkLocalAddress) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore exceptions and return null
        }
        return null
    }

    /**
     * Best-effort fetch of Firebase Cloud Messaging registration token
     * Kept here to follow the same Extension helper pattern as other device/app info getters
     */
    suspend fun getFirebaseToken(): String? = try {
        suspendCancellableCoroutine<String?> { cont ->
            try {
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task: com.google.android.gms.tasks.Task<String> ->
                        if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
                    }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    } catch (_: Exception) {
        null
    }
}
