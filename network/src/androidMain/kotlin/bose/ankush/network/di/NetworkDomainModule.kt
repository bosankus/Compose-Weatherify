package bose.ankush.network.di

import bose.ankush.network.api.FeedbackApiService
import bose.ankush.network.api.KtorFeedbackApiService
import bose.ankush.network.api.KtorLocationApiService
import bose.ankush.network.api.KtorPaymentApiService
import bose.ankush.network.api.KtorServiceApiService
import bose.ankush.network.api.KtorWeatherApiService
import bose.ankush.network.api.LocationApiService
import bose.ankush.network.api.PaymentApiService
import bose.ankush.network.api.ServiceApiService
import bose.ankush.network.api.WeatherApiService
import bose.ankush.network.auth.api.AuthApiService
import bose.ankush.network.auth.api.KtorAuthApiService
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.repository.AuthRepositoryImpl
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.repository.FeedbackRepository
import bose.ankush.network.repository.FeedbackRepositoryImpl
import bose.ankush.network.repository.LocationRepository
import bose.ankush.network.repository.LocationRepositoryImpl
import bose.ankush.network.repository.ServiceRepository
import bose.ankush.network.repository.ServiceRepositoryImpl
import bose.ankush.network.repository.WeatherRepository
import bose.ankush.network.repository.WeatherRepositoryImpl
import bose.ankush.network.util.AndroidNetworkConnectivity
import bose.ankush.network.util.NetworkConnectivity
import bose.ankush.network.utils.NetworkConstants
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkDomainModule: Module = module {
    single<NetworkConnectivity> { AndroidNetworkConnectivity(androidContext()) }
    single<HttpClient> { createHttpClient() }
    single<AuthApiService> { KtorAuthApiService(get(), NetworkConstants.WEATHER_BASE_URL) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<TokenManager> { TokenManager(get(), get()) }
    single<WeatherApiService> {
        KtorWeatherApiService(
            get(),
            get(),
            NetworkConstants.WEATHER_BASE_URL
        )
    }
    single<WeatherRepository> { WeatherRepositoryImpl(get(), get()) }
    single<FeedbackApiService> {
        KtorFeedbackApiService(
            get(),
            get(),
            NetworkConstants.WEATHER_BASE_URL
        )
    }
    single<FeedbackRepository> { FeedbackRepositoryImpl(get(), get()) }
    single<LocationApiService> {
        KtorLocationApiService(
            get(),
            get(),
            NetworkConstants.WEATHER_BASE_URL
        )
    }
    single<LocationRepository> { LocationRepositoryImpl(get()) }
    single<ServiceApiService> {
        KtorServiceApiService(
            get(),
            NetworkConstants.WEATHER_BASE_URL
        )
    }
    single<ServiceRepository> { ServiceRepositoryImpl(get()) }
    single<PaymentApiService> {
        KtorPaymentApiService(
            get(),
            get(),
            NetworkConstants.WEATHER_BASE_URL
        )
    }
}
