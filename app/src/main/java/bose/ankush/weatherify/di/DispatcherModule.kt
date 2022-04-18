package bose.ankush.weatherify.di

import bose.ankush.weatherify.dispatcher.AppDispatcher
import bose.ankush.weatherify.dispatcher.DispatcherProvider
import bose.ankush.weatherify.model.network.ApiService
import bose.ankush.weatherify.model.repository.WeatherRepository
import bose.ankush.weatherify.model.repository.WeatherRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Singleton
    @Provides
    fun provideDispatcherProvider(): DispatcherProvider = AppDispatcher()

    @Singleton
    @Provides
    fun provideWeatherRepository(
        apiService: ApiService,
        dispatcherProvider: DispatcherProvider
    ): WeatherRepository =
        WeatherRepositoryImpl(apiService, dispatcherProvider)
}