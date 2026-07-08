package bose.ankush.storage.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import bose.ankush.storage.EncryptedTokenStorageImpl
import bose.ankush.storage.LocationPreferencesStorageImpl
import bose.ankush.storage.WeatherStorageImpl
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.api.TokenStorage
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.common.LOCATION_PREFERENCES_FILE_NAME
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual val storageDomainModule: Module =
    module {
        single<TokenStorage> { EncryptedTokenStorageImpl() }
        single<WeatherStorage> { WeatherStorageImpl() }
        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = { (NSHomeDirectory() + "/" + LOCATION_PREFERENCES_FILE_NAME).toPath() },
            )
        }
        single<LocationPreferencesStorage> { LocationPreferencesStorageImpl(get()) }
    }
