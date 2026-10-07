package bose.ankush.weatherify

import android.app.Application
import bose.ankush.analytics.ErrorReporter
import bose.ankush.analytics.di.analyticsPlatformModule
import bose.ankush.auth.di.authDomainModule
import bose.ankush.auth.di.authViewModelModule
import bose.ankush.finder.di.finderDomainModule
import bose.ankush.finder.di.finderViewModelModule
import bose.ankush.home.di.homeDomainModule
import bose.ankush.home.di.homePlatformModule
import bose.ankush.home.di.homeViewModelModule
import bose.ankush.network.di.networkDomainModule
import bose.ankush.network.di.appBackgroundSourceModule
import bose.ankush.payment.di.paymentDomainModule
import bose.ankush.payment.di.paymentViewModelModule
import bose.ankush.settings.di.settingsViewModelModule
import bose.ankush.storage.di.storageDomainModule
import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.base.logging.CrashlyticsTree
import bose.ankush.weatherify.di.appNetworkConfigKoinModule
import bose.ankush.weatherify.di.appNotificationKoinModule
import bose.ankush.weatherify.di.appPaymentKoinModule
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class WeatherifyApplication : Application() {
    // Resolved lazily, so this is safe to declare before startKoin() runs.
    private val errorReporter: ErrorReporter by inject()

    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
        initKoin()
        initializeCrashlytics()
        enableTimber()
        subscribeToTopics()
    }

    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
    }

    private fun initializeCrashlytics() {
        errorReporter.setEnabled(!BuildConfig.DEBUG)
        errorReporter.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
        errorReporter.setCustomKey("version_name", BuildConfig.VERSION_NAME)
    }

    private fun enableTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree(errorReporter))
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

    private fun initKoin() {
        startKoin {
            androidContext(this@WeatherifyApplication)
            modules(
                listOf(
                    analyticsPlatformModule,
                    storageDomainModule,
                    networkDomainModule,
                    paymentDomainModule,
                    paymentViewModelModule,
                    appPaymentKoinModule,
                    appBackgroundSourceModule,
                    appNetworkConfigKoinModule,
                    appNotificationKoinModule,
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
}
