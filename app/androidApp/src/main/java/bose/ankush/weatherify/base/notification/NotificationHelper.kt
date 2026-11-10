package bose.ankush.weatherify.base.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import bose.ankush.weatherify.R

class NotificationHelper(
    private val context: Context,
) {
    /** Android 8+ drops every notification posted to a channel that was never created. */
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    DEFAULT_CHANNEL_ID,
                    context.getString(R.string.notification_channel_default_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description =
                        context.getString(R.string.notification_channel_default_description)
                },
                NotificationChannel(
                    ALERTS_CHANNEL_ID,
                    context.getString(R.string.notification_channel_alerts_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description =
                        context.getString(R.string.notification_channel_alerts_description)
                },
            ),
        )
    }

    /** The channel to post to: [requested] when it exists on this device, else the default. */
    fun resolveChannel(requested: String?): String {
        val manager = context.getSystemService(NotificationManager::class.java)
        return requested?.takeIf { manager?.getNotificationChannel(it) != null }
            ?: DEFAULT_CHANNEL_ID
    }

    /**
     * With [image] the expanded notification shows it full width and the collapsed one shows it as
     * a thumbnail; without, long [message]s expand instead of being cut to one line.
     */
    fun getNotificationBuilder(
        channelId: String,
        title: String,
        message: String,
        image: Bitmap? = null,
        largeIcon: Bitmap? = null,
    ): NotificationCompat.Builder {
        val style =
            if (image != null) {
                NotificationCompat
                    .BigPictureStyle()
                    .bigPicture(image)
                    .bigLargeIcon(largeIcon)
                    .setSummaryText(message)
            } else {
                NotificationCompat.BigTextStyle().bigText(message)
            }
        return NotificationCompat
            .Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(message)
            .setLargeIcon(largeIcon ?: image)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }

    companion object {
        const val DEFAULT_CHANNEL_ID = "weatherify_notifications"
        const val ALERTS_CHANNEL_ID = "weather_alerts"
    }
}
