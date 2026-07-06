package bose.ankush.home.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import bose.ankush.home.data.location.AndroidHomeGeocoder
import bose.ankush.home.data.location.DeviceLocationClient
import bose.ankush.home.data.preferences.HomeWeatherPreferences
import bose.ankush.home.data.remoteconfig.FirebaseHomeRemoteConfigGate
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import com.google.android.gms.location.LocationServices
import okio.Path.Companion.toPath
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
        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    androidContext().filesDir.resolve(HomeWeatherPreferences.FILE_NAME).absolutePath.toPath()
                },
            )
        }
        single<HomeRemoteConfigGate> { FirebaseHomeRemoteConfigGate() }
        single<HomeGeocoder> { AndroidHomeGeocoder(androidContext()) }
    }
