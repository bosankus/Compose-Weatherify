package bose.ankush.analytics.di

import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.analytics.ErrorReporter
import bose.ankush.analytics.FirebaseAnalyticsTracker
import bose.ankush.analytics.NoOpErrorReporter
import org.koin.core.module.Module
import org.koin.dsl.module

actual val analyticsPlatformModule: Module =
    module {
        single<AnalyticsTracker> { FirebaseAnalyticsTracker() }
        single<ErrorReporter> { NoOpErrorReporter() }
    }
