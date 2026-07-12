package bose.ankush.iosapp

import bose.ankush.analytics.di.analyticsPlatformModule
import bose.ankush.auth.di.authDomainModule
import bose.ankush.auth.di.authViewModelModule
import bose.ankush.finder.di.finderDomainModule
import bose.ankush.finder.di.finderViewModelModule
import bose.ankush.home.di.homeDomainModule
import bose.ankush.home.di.homePlatformModule
import bose.ankush.home.di.homeViewModelModule
import bose.ankush.home.di.initializeFirebase
import bose.ankush.iosapp.generated.IosSecrets
import bose.ankush.network.di.networkDomainModule
import bose.ankush.payment.di.paymentDomainModule
import bose.ankush.payment.di.paymentViewModelModule
import bose.ankush.payment.domain.config.PaymentConfig
import bose.ankush.settings.di.settingsViewModelModule
import bose.ankush.storage.di.storageDomainModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

// Key is generated at build time from secrets.properties/secrets.defaults.properties
// into IosSecrets — see the codegen step in app/iosApp/build.gradle.kts, which mirrors
// how androidApp gets BuildConfig.RAZORPAY_KEY from the same secrets file.
private val iosPaymentConfigModule =
    module {
        single<PaymentConfig> {
            object : PaymentConfig {
                override val razorpayKey: String get() = IosSecrets.RAZORPAY_KEY
            }
        }
    }

// Mirrors WeatherifyApplication.initKoin() on Android.
// Named startWeatherifyKoin, not initKoin: Kotlin/Native's Objective-C exporter
// renames any "init*" top-level function to "doInit*" to avoid clashing with
// ObjC's `init` initializer convention — that surprised Swift call sites, so the
// Kotlin-side name now matches what Swift actually sees.
fun startWeatherifyKoin() {
    initializeFirebase()
    startKoin {
        modules(
            listOf(
                analyticsPlatformModule,
                storageDomainModule,
                networkDomainModule,
                paymentDomainModule,
                paymentViewModelModule,
                iosPaymentConfigModule,
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
