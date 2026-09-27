package bose.ankush.weatherify.di

import bose.ankush.weatherify.base.notification.NotificationHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val appNotificationKoinModule: Module =
    module {
        single { NotificationHelper(androidContext()) }
    }
