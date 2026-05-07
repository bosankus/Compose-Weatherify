package bose.ankush.weatherify.di

import android.content.Context
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.common.AndroidNetworkConnectivity
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.di.createAuthRepository
import bose.ankush.network.di.createFeedbackRepository
import bose.ankush.network.di.createLocationRepository
import bose.ankush.network.di.createServiceRepository
import bose.ankush.network.di.createTokenManager
import bose.ankush.network.di.createWeatherRepository
import bose.ankush.network.domain.SavedLocationsUseCase
import bose.ankush.network.domain.SearchPlacesUseCase
import bose.ankush.network.repository.FeedbackRepository
import bose.ankush.network.repository.LocationRepository
import bose.ankush.network.repository.ServiceRepository
import bose.ankush.network.repository.WeatherRepository
import bose.ankush.storage.api.TokenStorage
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
        @ApplicationContext context: Context,
    ): NetworkConnectivity = AndroidNetworkConnectivity(context)

    /**
     * Provides WeatherRepository implementation from the network module
     * Uses TokenStorage for JWT authentication in API requests
     */
    @Provides
    @Singleton
    fun provideWeatherRepository(
        networkConnectivity: NetworkConnectivity,
        tokenStorage: TokenStorage,
    ): WeatherRepository = createWeatherRepository(networkConnectivity, tokenStorage)

    /**
     * Provides AuthRepository implementation from the network module
     */
    @Provides
    @Singleton
    fun provideAuthRepository(tokenStorage: TokenStorage): AuthRepository =
        createAuthRepository(tokenStorage)

    /**
     * Provides TokenManager singleton for use in ViewModels.
     * Shares the same AuthRepository singleton used elsewhere in the app.
     */
    @Provides
    @Singleton
    fun provideTokenManager(
        tokenStorage: TokenStorage,
        authRepository: AuthRepository,
    ): TokenManager = createTokenManager(tokenStorage, authRepository)

    /**
     * Provides FeedbackRepository implementation from the network module
     */
    @Provides
    @Singleton
    fun provideFeedbackRepository(
        networkConnectivity: NetworkConnectivity,
        tokenStorage: TokenStorage,
    ): FeedbackRepository = createFeedbackRepository(networkConnectivity, tokenStorage)

    /**
     * Provides LocationRepository for saved favourite locations (premium feature).
     */
    @Provides
    @Singleton
    fun provideLocationRepository(tokenStorage: TokenStorage): LocationRepository =
        createLocationRepository(tokenStorage)

    /**
     * Provides ServiceRepository for premium service subscriptions.
     * Uses basic HTTP client; /services/public endpoint requires no authentication.
     */
    @Provides
    @Singleton
    fun provideServiceRepository(): ServiceRepository = createServiceRepository()

    /**
     * Provides SearchPlacesUseCase for searching places.
     */
    @Provides
    @Singleton
    fun provideSearchPlacesUseCase(repository: LocationRepository): SearchPlacesUseCase =
        SearchPlacesUseCase(repository)

    /**
     * Provides SavedLocationsUseCase for managing saved locations.
     */
    @Provides
    @Singleton
    fun provideSavedLocationsUseCase(repository: LocationRepository): SavedLocationsUseCase =
        SavedLocationsUseCase(repository)
}
