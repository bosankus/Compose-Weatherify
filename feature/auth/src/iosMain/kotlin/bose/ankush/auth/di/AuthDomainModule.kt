package bose.ankush.auth.di

import bose.ankush.auth.domain.DeviceInfoProvider
import bose.ankush.auth.domain.IosDeviceInfoProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual val authDomainModule: Module =
    module {
        single<DeviceInfoProvider> { IosDeviceInfoProvider() }
    }
