package bose.ankush.weatherify.base.notification

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.repository.ACCOUNT_PHOTO_CHANGED_PUSH
import bose.ankush.network.repository.AccountRepository
import bose.ankush.network.repository.PushTokenRegistration
import bose.ankush.weatherify.R
import bose.ankush.weatherify.WeatherifyApplication
import bose.ankush.weatherify.presentation.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

class WeatherifyMessagingService : FirebaseMessagingService() {
    private val notificationHelper: NotificationHelper by inject()
    private val accountRepository: AccountRepository by inject()
    private val authRepository: AuthRepository by inject()
    private val pushTokens: PushTokenRegistration by inject()

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        Timber.d("Refreshed FCM token: $installationId")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        WeatherAlertsTopic.onTokenRotated(applicationContext)
        (application as WeatherifyApplication).appScope.launch {
            if (authRepository.isLoggedIn().first()) pushTokens.register(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("Message data payload: ${remoteMessage.data}")

        if (remoteMessage.data[PUSH_TYPE_KEY] == ACCOUNT_PHOTO_CHANGED_PUSH) {
            refreshAccount()
            return
        }

        val notification = remoteMessage.notification
        val data = remoteMessage.data
        val title = notification?.title ?: data[TITLE_KEY] ?: getString(R.string.app_name)
        val message = notification?.body ?: data[MESSAGE_KEY] ?: data[BODY_KEY]
        if (message.isNullOrBlank()) {
            // A data-only push with nothing to show, e.g. a type this build doesn't know yet.
            Timber.d("Push without displayable content: $data")
            return
        }

        val imageUrl = notification?.imageUrl?.toString() ?: data[IMAGE_KEY]
        val iconUrl = data[ICON_KEY]
        val channelId = notification?.channelId ?: data[CHANNEL_KEY]
        sendNotification(
            title = title,
            message = message,
            image = imageUrl?.let(::downloadBitmap),
            largeIcon = iconUrl?.let(::downloadBitmap),
            channelId = notificationHelper.resolveChannel(channelId),
            data = data,
        )
    }

    /**
     * Silent: the photo changed on another device. Fetching the account publishes the new photo
     * URL to every screen, and its new path drops the old photo from the cache. This runs on
     * FCM's worker thread, which allows blocking for a few seconds.
     */
    private fun refreshAccount() {
        runBlocking {
            withTimeoutOrNull(ACCOUNT_REFRESH_TIMEOUT_MS) { accountRepository.getAccount() }
                ?.onFailure { Timber.w(it, "Account refresh after push failed") }
        }
    }

    private fun sendNotification(
        title: String,
        message: String,
        image: Bitmap?,
        largeIcon: Bitmap?,
        channelId: String,
        data: Map<String, String>,
    ) {
        val notifications = NotificationManagerCompat.from(this)
        if (!notifications.areNotificationsEnabled()) {
            Timber.d("Notifications are off; dropping push \"$title\"")
            return
        }

        val notificationId = System.currentTimeMillis().toInt()
        // The push's data rides along as extras, so MainActivity can read it on open.
        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                data.forEach { (key, value) -> putExtra(key, value) }
            }
        // A request code per notification, so each tap opens with its own extras.
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notificationBuilder =
            notificationHelper
                .getNotificationBuilder(
                    channelId = channelId,
                    title = title,
                    message = message,
                    image = image,
                    largeIcon = largeIcon,
                ).setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        try {
            notifications.notify(notificationId, notificationBuilder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check above and here.
            Timber.w(e, "Notification permission missing")
        }
    }

    /** Runs on FCM's worker thread, which allows blocking; a failed image just leaves it out. */
    private fun downloadBitmap(url: String): Bitmap? {
        if (!url.startsWith("https://")) return null
        return runCatching {
            val connection =
                (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = IMAGE_TIMEOUT_MS
                    readTimeout = IMAGE_TIMEOUT_MS
                }
            try {
                val bytes = connection.inputStream.use { it.readBytes() }
                decodeSampled(bytes)
            } finally {
                connection.disconnect()
            }
        }.onFailure { Timber.w(it, "Notification image download failed: $url") }
            .getOrNull()
    }

    /** Decodes no larger than needed, so a huge image can't blow the notification's size limit. */
    private fun decodeSampled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_IMAGE_PX || bounds.outHeight / (sample * 2) >= MAX_IMAGE_PX) {
            sample *= 2
        }
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    private companion object {
        const val PUSH_TYPE_KEY = "type"
        const val ACCOUNT_REFRESH_TIMEOUT_MS = 8_000L
        const val TITLE_KEY = "title"
        const val MESSAGE_KEY = "message"
        const val BODY_KEY = "body"
        const val IMAGE_KEY = "image"
        const val ICON_KEY = "icon"
        const val CHANNEL_KEY = "channel_id"
        const val IMAGE_TIMEOUT_MS = 5_000
        const val MAX_IMAGE_PX = 1024
    }
}
