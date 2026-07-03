package bose.ankush.auth.domain

import android.content.Context
import android.os.Build
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.NetworkInterface
import kotlin.coroutines.resume

internal class AndroidDeviceInfoProvider(
    private val context: Context,
) : DeviceInfoProvider {
    override fun getDeviceModel(): String = Build.MODEL

    override fun getOperatingSystem(): String = OPERATING_SYSTEM

    override fun getOsVersion(): String = Build.VERSION.RELEASE

    override fun getAppVersion(): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""

    override fun getRegistrationSource(): String = REGISTRATION_SOURCE

    override fun getIpAddress(): String? =
        runCatching {
            NetworkInterface
                .getNetworkInterfaces()
                .asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                ?.hostAddress
        }.getOrNull()

    override fun getCurrentUtcTimestamp(): String = currentUtcTimestamp()

    override suspend fun getFirebaseToken(): String? =
        runCatching {
            suspendCancellableCoroutine { cont ->
                FirebaseMessaging
                    .getInstance()
                    .token
                    .addOnCompleteListener { task: Task<String> ->
                        if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
                    }
            }
        }.getOrNull()

    private companion object {
        const val OPERATING_SYSTEM = "Android"
        const val REGISTRATION_SOURCE = "Android App"
    }
}
