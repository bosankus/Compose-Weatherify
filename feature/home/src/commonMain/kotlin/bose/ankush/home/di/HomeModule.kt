package bose.ankush.home.di

import bose.ankush.home.HomeLocationCoordinator
import bose.ankush.home.HomeSavedPlacesEntry
import bose.ankush.home.HomeSessionCleaner
import bose.ankush.home.data.HomeLocationCoordinatorImpl
import bose.ankush.home.data.HomeSavedPlacesEntryImpl
import bose.ankush.home.data.HomeSessionCleanerImpl
import bose.ankush.home.data.repository.WeatherRepositoryImpl
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.home.domain.usecase.CreateNearbyEvent
import bose.ankush.home.domain.usecase.FindNearestSavedPlace
import bose.ankush.home.domain.usecase.GetAirQuality
import bose.ankush.home.domain.usecase.GetNearbyEvents
import bose.ankush.home.domain.usecase.GetWeatherReport
import bose.ankush.home.domain.usecase.ObserveAccountPhotoUrl
import bose.ankush.home.domain.usecase.RefreshAccount
import bose.ankush.home.domain.usecase.RefreshWeatherReport
import bose.ankush.home.presentation.HomeViewModel
import bose.ankush.home.presentation.account.AccountAvatarViewModel
import bose.ankush.home.presentation.nearby.NearbyViewModel
import bose.ankush.home.presentation.places.SavedPlacesViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Platform-specific bindings: [bose.ankush.home.domain.location.LocationClient],
 * [bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate]. */
expect val homePlatformModule: Module

/** Relies on `storageDomainModule` (module `:storage`) being loaded for
 * `bose.ankush.storage.api.LocationPreferencesStorage`. */
val homeDomainModule: Module =
    module {
        single<WeatherRepository> { WeatherRepositoryImpl(get(), get(), get(), get()) }
        factory { GetWeatherReport(get()) }
        factory { RefreshWeatherReport(get()) }
        factory { GetAirQuality(get()) }
        factory { GetNearbyEvents(get()) }
        factory { FindNearestSavedPlace(get()) }
        factory { CreateNearbyEvent(get()) }
        factory { ObserveAccountPhotoUrl(get()) }
        factory { RefreshAccount(get()) }
        single<HomeLocationCoordinator> { HomeLocationCoordinatorImpl(get()) }
        single { HomeSavedPlacesEntryImpl() } bind HomeSavedPlacesEntry::class
        single<HomeSessionCleaner> { HomeSessionCleanerImpl(get(), get(), get()) }
    }

val homeViewModelModule: Module =
    module {
        viewModelOf(::HomeViewModel)
        viewModelOf(::NearbyViewModel)
        viewModelOf(::AccountAvatarViewModel)
        viewModelOf(::SavedPlacesViewModel)
    }
