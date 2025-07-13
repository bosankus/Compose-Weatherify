package bose.ankush.weatherify.di

import android.content.Context
import bose.ankush.network.common.AndroidNetworkConnectivity
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.di.createWeatherRepository
import bose.ankush.network.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for providing network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides NetworkConnectivity implementation
     */
    @Provides
    @Singleton
    fun provideNetworkConnectivity(
        @ApplicationContext context: Context
    ): NetworkConnectivity {
        return AndroidNetworkConnectivity(context)
    }

    /**
     * Provides WeatherRepository implementation from the network module
     */
    @Provides
    @Singleton
    fun provideWeatherRepository(
        networkConnectivity: NetworkConnectivity
    ): WeatherRepository {
        return createWeatherRepository(networkConnectivity)
    }
}