package bose.ankush.weatherify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import bose.ankush.weatherify.base.location.LocationService.Companion.NOTIFICATION_CHANNEL_ID
import bose.ankush.weatherify.base.location.LocationService.Companion.NOTIFICATION_NAME
import bose.ankush.weatherify.domain.remote_config.RemoteConfigService
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

@HiltAndroidApp
class WeatherifyApplication : WeatherifyApplicationCore() {

    @Inject
    lateinit var remoteConfigService: RemoteConfigService

    override fun onCreate() {
        super.onCreate()
        enableTimber()
        createNotificationChannel()
        initializeRemoteConfig()
    }

    private fun initializeRemoteConfig() {
        remoteConfigService.initialize()
    }

    private fun enableTimber() {
        Timber.plant(Timber.DebugTree())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
