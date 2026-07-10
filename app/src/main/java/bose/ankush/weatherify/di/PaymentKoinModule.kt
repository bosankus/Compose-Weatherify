package bose.ankush.weatherify.di

import bose.ankush.payment.domain.config.PaymentConfig
import bose.ankush.weatherify.base.config.AndroidAppConfig
import bose.ankush.weatherify.payment.config.AppConfigPaymentConfig
import org.koin.core.module.Module
import org.koin.dsl.module

val appPaymentKoinModule: Module =
    module {
        single<PaymentConfig> { AppConfigPaymentConfig(AndroidAppConfig()) }
    }
