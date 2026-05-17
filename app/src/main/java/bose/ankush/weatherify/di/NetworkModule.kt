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

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideNetworkConnectivity(
        @ApplicationContext context: Context,
    ): NetworkConnectivity = AndroidNetworkConnectivity(context)

    @Provides
    @Singleton
    fun provideWeatherRepository(
        networkConnectivity: NetworkConnectivity,
        tokenStorage: TokenStorage,
    ): WeatherRepository = createWeatherRepository(networkConnectivity, tokenStorage)

    @Provides
    @Singleton
    fun provideAuthRepository(tokenStorage: TokenStorage): AuthRepository =
        createAuthRepository(tokenStorage)

    @Provides
    @Singleton
    fun provideTokenManager(
        tokenStorage: TokenStorage,
        authRepository: AuthRepository,
    ): TokenManager = createTokenManager(tokenStorage, authRepository)

    @Provides
    @Singleton
    fun provideFeedbackRepository(
        networkConnectivity: NetworkConnectivity,
        tokenStorage: TokenStorage,
    ): FeedbackRepository = createFeedbackRepository(networkConnectivity, tokenStorage)

    @Provides
    @Singleton
    fun provideLocationRepository(tokenStorage: TokenStorage): LocationRepository =
        createLocationRepository(tokenStorage)

    @Provides
    @Singleton
    fun provideServiceRepository(): ServiceRepository = createServiceRepository()

    @Provides
    @Singleton
    fun provideSearchPlacesUseCase(repository: LocationRepository): SearchPlacesUseCase =
        SearchPlacesUseCase(repository)

    @Provides
    @Singleton
    fun provideSavedLocationsUseCase(repository: LocationRepository): SavedLocationsUseCase =
        SavedLocationsUseCase(repository)
}
