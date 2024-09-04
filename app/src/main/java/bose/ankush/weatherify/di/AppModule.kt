package bose.ankush.weatherify.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import bose.ankush.weatherify.base.common.WEATHER_DATABASE_NAME
import bose.ankush.weatherify.data.room.JsonParser
import bose.ankush.weatherify.data.room.Parser
import bose.ankush.weatherify.data.room.weather.WeatherDataModelConverters
import bose.ankush.weatherify.data.room.weather.WeatherDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context =
        application.applicationContext

    @Singleton
    @Provides
    fun providesParser(gson: Gson): Parser = JsonParser(gson)

    @Singleton
    @Provides
    fun provideTypeWeatherDataModelConverters(parser: Parser): WeatherDataModelConverters =
        WeatherDataModelConverters(parser)

    @Singleton
    @Provides
    fun providesWeatherDatabase(
        @ApplicationContext context: Context,
        weatherDataModelConverters: WeatherDataModelConverters
    ): WeatherDatabase {
        return Room.databaseBuilder(
            context,
            WeatherDatabase::class.java,
            WEATHER_DATABASE_NAME
        )
            .addTypeConverter(weatherDataModelConverters)
            .build()
    }
}