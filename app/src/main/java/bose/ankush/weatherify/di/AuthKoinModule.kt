package bose.ankush.weatherify.di

import bose.ankush.auth.domain.DeviceInfoProvider
import bose.ankush.weatherify.base.common.AndroidDeviceInfoProvider
import org.koin.core.module.Module
import org.koin.dsl.module

val appAuthKoinModule: Module =
    module {
        single<DeviceInfoProvider> { AndroidDeviceInfoProvider() }
    }
