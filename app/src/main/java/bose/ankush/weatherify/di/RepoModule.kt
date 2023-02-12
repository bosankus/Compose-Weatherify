package bose.ankush.weatherify.di

import android.content.Context
import bose.ankush.weatherify.data.remote.api.OpenWeatherApiService
import bose.ankush.weatherify.data.repository.CityRepositoryImpl
import bose.ankush.weatherify.data.repository.WeatherRepositoryImpl
import bose.ankush.weatherify.dispatcher.DispatcherProvider
import bose.ankush.weatherify.domain.repository.CityRepository
import bose.ankush.weatherify.domain.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RepoModule {

    @Singleton
    @Provides
    fun provideWeatherRepository(
        openWeatherApiService: OpenWeatherApiService,
        dispatcherProvider: DispatcherProvider
    ): WeatherRepository =
        WeatherRepositoryImpl(openWeatherApiService, dispatcherProvider)

    @Singleton
    @Provides
    fun provideCityRepository(
        context: Context
    ): CityRepository =
        CityRepositoryImpl(context)
}
