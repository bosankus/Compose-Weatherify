package bose.ankush.home.di

import bose.ankush.home.HomeLocationCoordinator
import bose.ankush.home.HomeSessionCleaner
import bose.ankush.home.data.HomeLocationCoordinatorImpl
import bose.ankush.home.data.HomeSessionCleanerImpl
import bose.ankush.home.data.preferences.HomeWeatherPreferences
import bose.ankush.home.data.repository.WeatherRepositoryImpl
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.home.domain.usecase.GetAirQuality
import bose.ankush.home.domain.usecase.GetWeatherReport
import bose.ankush.home.domain.usecase.RefreshWeatherReport
import bose.ankush.home.presentation.HomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Platform-specific bindings: [bose.ankush.home.domain.location.LocationClient],
 * `DataStore<Preferences>`, [bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate]. */
expect val homePlatformModule: Module

val homeDomainModule: Module =
    module {
        single<WeatherRepository> { WeatherRepositoryImpl(get(), get()) }
        factory { GetWeatherReport(get()) }
        factory { RefreshWeatherReport(get()) }
        factory { GetAirQuality(get()) }
        single { HomeWeatherPreferences(get()) }
        single<HomeLocationCoordinator> { HomeLocationCoordinatorImpl(get()) }
        single<HomeSessionCleaner> { HomeSessionCleanerImpl(get(), get()) }
    }

val homeViewModelModule: Module =
    module {
        viewModelOf(::HomeViewModel)
    }
