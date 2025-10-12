package bose.ankush.weatherify

import android.app.NotificationChannel
import android.app.NotificationManager
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
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Only log WARN, ERROR, and WTF in release; avoid verbose/debug/info
                    if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG || priority == android.util.Log.INFO) return
                    android.util.Log.println(priority, tag, message)
                }
            })
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
