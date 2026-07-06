package bose.ankush.home.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import bose.ankush.home.data.location.IosHomeGeocoder
import bose.ankush.home.data.location.IosLocationClient
import bose.ankush.home.data.preferences.HomeWeatherPreferences
import bose.ankush.home.data.remoteconfig.DefaultHomeRemoteConfigGate
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual val homePlatformModule: Module =
    module {
        single<LocationClient> { IosLocationClient() }
        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = { (NSHomeDirectory() + "/" + HomeWeatherPreferences.FILE_NAME).toPath() },
            )
        }
        single<HomeRemoteConfigGate> { DefaultHomeRemoteConfigGate() }
        single<HomeGeocoder> { IosHomeGeocoder() }
    }
