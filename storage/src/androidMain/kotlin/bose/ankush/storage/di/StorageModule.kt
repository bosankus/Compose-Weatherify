package bose.ankush.storage.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import bose.ankush.storage.EncryptedTokenStorageImpl
import bose.ankush.storage.LocationPreferencesStorageImpl
import bose.ankush.storage.PremiumStorageImpl
import bose.ankush.storage.WeatherStorageImpl
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.api.PremiumStorage
import bose.ankush.tokenstorage.api.TokenStorage
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.common.LOCATION_PREFERENCES_FILE_NAME
import bose.ankush.storage.common.PREMIUM_PREFERENCES_FILE_NAME
import bose.ankush.storage.room.WeatherDataModelConverters
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.room.createWeatherDatabase
import bose.ankush.storage.setApplicationContext
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val premiumPreferencesQualifier = named("premiumPreferencesDataStore")

actual val storageDomainModule: Module =
    module {
        single<WeatherDataModelConverters> { WeatherDataModelConverters() }
        single<WeatherDatabase> {
            createWeatherDatabase(
                context = androidContext(),
                converters = get(),
            )
        }
        single<WeatherStorage> { WeatherStorageImpl(get()) }
        single<TokenStorage> {
            setApplicationContext(androidContext())
            EncryptedTokenStorageImpl()
        }
        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    androidContext()
                        .filesDir
                        .resolve(LOCATION_PREFERENCES_FILE_NAME)
                        .absolutePath
                        .toPath()
                },
            )
        }
        single<LocationPreferencesStorage> { LocationPreferencesStorageImpl(get()) }
        single<DataStore<Preferences>>(premiumPreferencesQualifier) {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    androidContext()
                        .filesDir
                        .resolve("datastore")
                        .resolve(PREMIUM_PREFERENCES_FILE_NAME)
                        .absolutePath
                        .toPath()
                },
            )
        }
        single<PremiumStorage> { PremiumStorageImpl(get(premiumPreferencesQualifier)) }
    }
