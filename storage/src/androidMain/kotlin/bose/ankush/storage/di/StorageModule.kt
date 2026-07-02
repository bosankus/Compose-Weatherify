package bose.ankush.storage.di

import androidx.room.Room
import bose.ankush.storage.EncryptedTokenStorageImpl
import bose.ankush.storage.WeatherStorageImpl
import bose.ankush.storage.api.TokenStorage
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.common.WEATHER_DATABASE_NAME
import bose.ankush.storage.room.JsonParser
import bose.ankush.storage.room.Parser
import bose.ankush.storage.room.WeatherDataModelConverters
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.setApplicationContext
import com.google.gson.Gson
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
    }
