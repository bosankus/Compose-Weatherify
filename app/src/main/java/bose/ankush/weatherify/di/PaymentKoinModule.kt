package bose.ankush.weatherify.di

import bose.ankush.payment.domain.config.PaymentConfig
import bose.ankush.weatherify.BuildConfig
import org.koin.core.module.Module
import org.koin.dsl.module

val appPaymentKoinModule: Module =
    module {
        single<PaymentConfig> {
            object : PaymentConfig {
                override val razorpayKey: String get() = BuildConfig.RAZORPAY_KEY
            }
        }
    }
