package bose.ankush.home.di

import bose.ankush.home.data.location.AndroidHomeGeocoder
import bose.ankush.home.data.location.DeviceLocationClient
import bose.ankush.home.data.remoteconfig.FirebaseHomeRemoteConfigGate
import bose.ankush.home.data.wear.AndroidWeatherWearSync
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.repository.WeatherWearSync
import com.google.android.gms.location.LocationServices
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val homePlatformModule: Module =
    module {
        single<LocationClient> {
            DeviceLocationClient(
                context = androidContext(),
                client = LocationServices.getFusedLocationProviderClient(androidContext()),
            )
        }
        single<HomeRemoteConfigGate> { FirebaseHomeRemoteConfigGate() }
        single<HomeGeocoder> { AndroidHomeGeocoder(androidContext()) }
        single<WeatherWearSync> { AndroidWeatherWearSync(androidContext()) }
    }
