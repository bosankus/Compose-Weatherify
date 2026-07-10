package bose.ankush.weatherify.di

import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.repository.FeedbackRepository
import bose.ankush.network.repository.WeatherRepository
import bose.ankush.network.util.NetworkConnectivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koin.core.context.GlobalContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideNetworkConnectivity(): NetworkConnectivity = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideWeatherRepository(): WeatherRepository = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideTokenManager(): TokenManager = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideFeedbackRepository(): FeedbackRepository = GlobalContext.get().get()
}
