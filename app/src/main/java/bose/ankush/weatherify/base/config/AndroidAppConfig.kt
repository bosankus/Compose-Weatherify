package bose.ankush.weatherify.base.config

import bose.ankush.weatherify.BuildConfig

/** Android implementation of [AppConfig] backed by BuildConfig generated values. */
class AndroidAppConfig : AppConfig {
    override val razorpayKey: String get() = BuildConfig.RAZORPAY_KEY
}
