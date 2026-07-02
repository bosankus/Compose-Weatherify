package bose.ankush.auth.di

import bose.ankush.auth.domain.AndroidDeviceInfoProvider
import bose.ankush.auth.domain.DeviceInfoProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val authDomainModule: Module =
    module {
        single<DeviceInfoProvider> { AndroidDeviceInfoProvider(androidContext()) }
    }
