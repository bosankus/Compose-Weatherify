package bose.ankush.weatherify.di

import android.content.Context
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.storage.TokenStorage
import bose.ankush.network.common.AndroidNetworkConnectivity
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.di.createAuthRepository
import bose.ankush.network.di.createFeedbackRepository
import bose.ankush.network.di.createPaymentRepository
import bose.ankush.network.di.createWeatherRepository
import bose.ankush.network.repository.FeedbackRepository
import bose.ankush.network.repository.PaymentRepository
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
     * Uses TokenStorage for JWT authentication in API requests
     */
    @Provides
    @Singleton
    fun provideWeatherRepository(
        networkConnectivity: NetworkConnectivity,
        tokenStorage: TokenStorage
    ): WeatherRepository {
        return createWeatherRepository(networkConnectivity, tokenStorage)
    }

    /**
     * Provides PaymentRepository implementation from the network module
     */
    @Provides
    @Singleton
    fun providePaymentRepository(
        networkConnectivity: NetworkConnectivity,
        tokenStorage: TokenStorage
    ): PaymentRepository {
        return createPaymentRepository(networkConnectivity, tokenStorage)
    }

    /**
     * Provides AuthRepository implementation from the network module
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        tokenStorage: TokenStorage
    ): AuthRepository {
        return createAuthRepository(tokenStorage)
    }

    /**
     * Provides FeedbackRepository implementation from the network module
     */
    @Provides
    @Singleton
    fun provideFeedbackRepository(
        networkConnectivity: NetworkConnectivity,
        tokenStorage: TokenStorage
    ): FeedbackRepository {
        return createFeedbackRepository(networkConnectivity, tokenStorage)
    }
}
