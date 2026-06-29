package bose.ankush.weatherify.di

import bose.ankush.storage.api.TokenStorage
import bose.ankush.storage.api.WeatherStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koin.core.context.GlobalContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideWeatherStorage(): WeatherStorage = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideTokenStorage(): TokenStorage = GlobalContext.get().get()
}