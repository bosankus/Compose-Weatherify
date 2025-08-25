/*
Moved to app module: bose.ankush.storage.di.StorageModule
This file is kept as a comment-only placeholder to avoid Hilt in storage module.

import android.content.Context
import androidx.room.Room
import bose.ankush.network.auth.storage.TokenStorage
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.common.WEATHER_DATABASE_NAME
import bose.ankush.storage.impl.TokenStorageImpl
import bose.ankush.storage.impl.WeatherStorageImpl
import bose.ankush.storage.room.JsonParser
import bose.ankush.storage.room.WeatherDataModelConverters
import bose.ankush.storage.room.WeatherDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import bose.ankush.network.repository.WeatherRepository as NetworkWeatherRepository

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideJsonParser(gson: Gson): JsonParser = JsonParser(gson)

    @Provides
    @Singleton
    fun provideWeatherDataModelConverters(jsonParser: JsonParser): WeatherDataModelConverters {
        return WeatherDataModelConverters(jsonParser)
    }

    @Provides
    @Singleton
    fun provideWeatherDatabase(
        @ApplicationContext context: Context,
        converters: WeatherDataModelConverters
    ): WeatherDatabase {
        return Room.databaseBuilder(
            context,
            WeatherDatabase::class.java,
            WEATHER_DATABASE_NAME
        )
            .addTypeConverter(converters)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherStorage(
        networkRepository: NetworkWeatherRepository,
        weatherDatabase: WeatherDatabase
    ): WeatherStorage {
        return WeatherStorageImpl(networkRepository, weatherDatabase)
    }

    @Provides
    @Singleton
    fun provideTokenStorage(
        weatherDatabase: WeatherDatabase
    ): TokenStorage {
        return TokenStorageImpl(weatherDatabase)
    }
}

// Moved to app module: bose.ankush.storage.di.StorageModule
// This placeholder file intentionally left empty to avoid Hilt in storage module.
*/
