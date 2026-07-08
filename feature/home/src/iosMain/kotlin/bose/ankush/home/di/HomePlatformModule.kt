package bose.ankush.home.di

import bose.ankush.home.data.location.IosHomeGeocoder
import bose.ankush.home.data.location.IosLocationClient
import bose.ankush.home.data.remoteconfig.FirebaseHomeRemoteConfigGate
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import org.koin.core.module.Module
import org.koin.dsl.module

actual val homePlatformModule: Module =
    module {
        single<LocationClient> { IosLocationClient() }
        single<HomeRemoteConfigGate> { FirebaseHomeRemoteConfigGate() }
        single<HomeGeocoder> { IosHomeGeocoder() }
    }
