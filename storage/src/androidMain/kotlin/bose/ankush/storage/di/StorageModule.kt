package bose.ankush.storage.di

import android.content.Context
import androidx.room.Room
import bose.ankush.network.repository.WeatherRepository as NetworkWeatherRepository
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.common.WEATHER_DATABASE_NAME
import bose.ankush.storage.impl.WeatherStorageImpl
import bose.ankush.storage.room.JsonParser
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.room.WeatherDataModelConverters
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
}
