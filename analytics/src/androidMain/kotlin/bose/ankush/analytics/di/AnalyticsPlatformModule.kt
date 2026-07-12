package bose.ankush.analytics.di

import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.analytics.FirebaseAnalyticsTracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val analyticsPlatformModule: Module =
    module {
        single<AnalyticsTracker> { FirebaseAnalyticsTracker(androidContext()) }
    }
