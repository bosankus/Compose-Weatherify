package bose.ankush.weatherify.base.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import bose.ankush.weatherify.R
import bose.ankush.weatherify.presentation.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WeatherifyMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("Refreshed FCM token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("Message data payload: ${remoteMessage.data}")

        val title =
            remoteMessage.notification?.title
                ?: remoteMessage.data["title"]
                ?: getString(R.string.app_name)

        val message =
            remoteMessage.notification?.body
                ?: remoteMessage.data["message"]
                ?: remoteMessage.data.values.firstOrNull()
                ?: ""

        val customData = remoteMessage.data.filterKeys { it != "title" && it != "message" }
        if (customData.isNotEmpty()) {
            Timber.d("Custom data payload: $customData")
        }

        if (message.isNotBlank()) {
            sendNotification(title, message)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun sendNotification(
        title: String,
        message: String,
    ) {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notificationBuilder =
            notificationHelper
                .getNotificationBuilder(
                    channelId = NotificationHelper.DEFAULT_CHANNEL_ID,
                    title = title,
                    message = message,
                ).setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
