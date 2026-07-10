package bose.ankush.weatherify.di

import bose.ankush.payment.domain.config.PaymentConfig
import bose.ankush.payment.domain.store.PremiumStore
import bose.ankush.weatherify.base.config.AndroidAppConfig
import bose.ankush.weatherify.payment.config.AppConfigPaymentConfig
import bose.ankush.weatherify.payment.store.StoragePremiumStore
import org.koin.core.module.Module
import org.koin.dsl.module

val appPaymentKoinModule: Module =
    module {
        single<PremiumStore> { StoragePremiumStore(get()) }
        single<PaymentConfig> { AppConfigPaymentConfig(AndroidAppConfig()) }
    }
