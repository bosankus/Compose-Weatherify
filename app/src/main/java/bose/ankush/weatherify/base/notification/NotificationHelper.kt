package bose.ankush.weatherify.base.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import bose.ankush.weatherify.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper
    @Inject
    constructor(
        private val context: Context,
    ) {
        fun getNotificationBuilder(
            channelId: String,
            title: String,
            message: String,
        ): NotificationCompat.Builder =
            NotificationCompat
                .Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_home)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

        companion object {
            const val DEFAULT_CHANNEL_ID = "weatherify_notifications"
        }
    }
