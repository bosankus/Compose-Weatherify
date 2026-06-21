package bose.ankush.weatherify

import android.app.NotificationChannel
import android.app.NotificationManager
import bose.ankush.payment.di.featurePaymentModules
import bose.ankush.weatherify.base.location.LocationService.Companion.NOTIFICATION_CHANNEL_ID
import bose.ankush.weatherify.base.location.LocationService.Companion.NOTIFICATION_NAME
import bose.ankush.weatherify.di.appPaymentKoinModule
import bose.ankush.weatherify.domain.remote_config.RemoteConfigService
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WeatherifyApplication : WeatherifyApplicationCore() {
    @Inject
    lateinit var remoteConfigService: RemoteConfigService

    override fun onCreate() {
        super.onCreate()
        initKoin()
        enableTimber()
        initializeFirebase()
        createNotificationChannel()
        initializeRemoteConfig()
        subscribeToTopics()
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@WeatherifyApplication)
            modules(featurePaymentModules + appPaymentKoinModule(this@WeatherifyApplication))
        }
    }

    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
    }

    private fun subscribeToTopics() {
        FirebaseMessaging
            .getInstance()
            .subscribeToTopic("weather_alerts")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.e(task.exception, "Failed to subscribe to weather_alerts topic")
                } else {
                    Timber.d("Successfully subscribed to weather_alerts topic")
                }
            }
    }

    private fun initializeRemoteConfig() {
        remoteConfigService.initialize()
    }

    private fun enableTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(
                object : Timber.Tree() {
                    override fun log(
                        priority: Int,
                        tag: String?,
                        message: String,
                        t: Throwable?,
                    ) {
                        val isLowPriority =
                            priority == android.util.Log.VERBOSE ||
                                    priority == android.util.Log.DEBUG ||
                                    priority == android.util.Log.INFO
                        if (isLowPriority) return
                        android.util.Log.println(priority, tag, message)
                    }
                },
            )
        }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Channel for weather alerts and updates"
                enableVibration(true)
            }

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Timber.d("Notification channel created: $NOTIFICATION_CHANNEL_ID")
    }
}
