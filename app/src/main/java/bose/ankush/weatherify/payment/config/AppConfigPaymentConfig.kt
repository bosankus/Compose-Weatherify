package bose.ankush.weatherify.payment.config

import bose.ankush.payment.domain.config.PaymentConfig
import bose.ankush.weatherify.base.config.AppConfig

/**
 * Bridges [AppConfig] (Hilt-managed) into the [PaymentConfig] interface
 * consumed by [bose.ankush.payment.presentation.PaymentViewModel] (Koin-managed).
 */
internal class AppConfigPaymentConfig(private val appConfig: AppConfig) : PaymentConfig {
    override val razorpayKey: String get() = appConfig.razorpayKey
}
