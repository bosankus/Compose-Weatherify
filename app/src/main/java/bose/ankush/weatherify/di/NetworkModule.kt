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
 * Network module for Dagger Hilt that provides dependencies from the KMM network module
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun providesNetworkConnectivity(@ApplicationContext context: Context): NetworkConnectivity {
        return AndroidNetworkConnectivity(context)
    }

    @Singleton
    @Provides
    fun providesWeatherRepository(
        networkConnectivity: NetworkConnectivity
    ): WeatherRepository {
        // Use the factory method from the network module to create a repository
        return createWeatherRepository(networkConnectivity)
    }
}
