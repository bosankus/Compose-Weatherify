package bose.ankush.storage.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import bose.ankush.storage.EncryptedTokenStorageImpl
import bose.ankush.storage.LocationPreferencesStorageImpl
import bose.ankush.storage.WeatherStorageImpl
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.api.TokenStorage
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.common.LOCATION_PREFERENCES_FILE_NAME
import bose.ankush.storage.common.WEATHER_DATABASE_NAME
import bose.ankush.storage.room.JsonParser
import bose.ankush.storage.room.Parser
import bose.ankush.storage.room.WeatherDataModelConverters
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.setApplicationContext
import com.google.gson.Gson
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val storageDomainModule: Module =
    module {
        single<Gson> { Gson() }
        single<Parser> { JsonParser(get()) }
        single<WeatherDataModelConverters> { WeatherDataModelConverters(get()) }
        single<WeatherDatabase> {
            Room
                .databaseBuilder(
                    androidContext(),
                    WeatherDatabase::class.java,
                    WEATHER_DATABASE_NAME,
                ).addTypeConverter(get<WeatherDataModelConverters>())
                .fallbackToDestructiveMigration(false)
                .build()
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
    }
