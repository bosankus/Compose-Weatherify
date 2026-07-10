package bose.ankush.weatherify

import android.app.Application
import bose.ankush.auth.di.authDomainModule
import bose.ankush.auth.di.authViewModelModule
import bose.ankush.finder.di.finderDomainModule
import bose.ankush.finder.di.finderViewModelModule
import bose.ankush.home.di.homeDomainModule
import bose.ankush.home.di.homePlatformModule
import bose.ankush.home.di.homeViewModelModule
import bose.ankush.network.di.networkDomainModule
import bose.ankush.payment.di.paymentDomainModule
import bose.ankush.payment.di.paymentViewModelModule
import bose.ankush.settings.di.settingsViewModelModule
import bose.ankush.storage.di.storageDomainModule
import bose.ankush.weatherify.di.appPaymentKoinModule
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

@HiltAndroidApp
class WeatherifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
        enableTimber()
        initializeFirebase()
        subscribeToTopics()
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@WeatherifyApplication)
            modules(
                listOf(
                    storageDomainModule,
                    networkDomainModule,
                    paymentDomainModule,
                    paymentViewModelModule,
                    appPaymentKoinModule,
                    authViewModelModule,
                    authDomainModule,
                    finderDomainModule,
                    finderViewModelModule,
                    homePlatformModule,
                    homeDomainModule,
                    homeViewModelModule,
                    settingsViewModelModule,
                ),
            )
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
}
