package bose.ankush.weatherify.di

import android.content.Context
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.data.repository.CityRepositoryImpl
import bose.ankush.weatherify.data.repository.WeatherRepositoryImpl
import bose.ankush.weatherify.domain.repository.CityRepository
import bose.ankush.weatherify.domain.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import bose.ankush.network.repository.WeatherRepository as NetworkWeatherRepository

@Module
@InstallIn(SingletonComponent::class)
object RepoModule {
    @Singleton
    @Provides
    fun provideWeatherRepository(
        networkRepository: NetworkWeatherRepository,
        weatherStorage: WeatherStorage,
        dispatcherProvider: DispatcherProvider,
    ): WeatherRepository =
        WeatherRepositoryImpl(
            networkRepository,
            weatherStorage,
            dispatcherProvider,
        )

    @Singleton
    @Provides
    fun provideCityRepository(context: Context): CityRepository = CityRepositoryImpl(context)
}
