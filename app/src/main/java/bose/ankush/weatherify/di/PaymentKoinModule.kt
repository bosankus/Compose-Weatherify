package bose.ankush.weatherify.di

import android.content.Context
import bose.ankush.network.common.AndroidNetworkConnectivity
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.api.PaymentApiService
import bose.ankush.network.di.createPaymentApiService
import bose.ankush.payment.domain.config.PaymentConfig
import bose.ankush.payment.domain.store.PremiumStore
import bose.ankush.weatherify.payment.config.AppConfigPaymentConfig
import bose.ankush.weatherify.payment.store.PreferenceManagerPremiumStore
import dagger.hilt.android.EntryPointAccessors
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Builds the app-level Koin module that provides platform-specific bindings required by
 * [bose.ankush.payment.di.featurePaymentModules].
 *
 * Called from [bose.ankush.weatherify.WeatherifyApplication] **after** Hilt is initialized
 * (i.e. after super.onCreate()), so [EntryPointAccessors] is safe to use here.
 */
fun appPaymentKoinModule(context: Context): Module {
    val bridge = EntryPointAccessors.fromApplication(
        context,
        PaymentKoinBridgeEntryPoint::class.java,
    )
    val tokenStorage = bridge.tokenStorage()
    val preferenceManager = bridge.preferenceManager()
    val appConfig = bridge.appConfig()

    return module {
        // NetworkConnectivity: stateless — safe to create a fresh instance for Koin
        single<NetworkConnectivity> { AndroidNetworkConnectivity(androidContext()) }

        // PaymentApiService: uses authenticated HTTP client from the network module
        single<PaymentApiService> { createPaymentApiService(tokenStorage) }

        // PremiumStore: wraps the Hilt-managed PreferenceManager singleton
        single<PremiumStore> { PreferenceManagerPremiumStore(preferenceManager) }

        // PaymentConfig: wraps the Hilt-managed AppConfig singleton
        single<PaymentConfig> { AppConfigPaymentConfig(appConfig) }
    }
}
