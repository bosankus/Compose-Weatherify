package bose.ankush.home.di

import bose.ankush.home.HomeLocationCoordinator
import bose.ankush.home.HomeSavedPlacesEntry
import bose.ankush.home.HomeSessionCleaner
import bose.ankush.home.data.HomeLocationCoordinatorImpl
import bose.ankush.home.data.HomeSavedPlacesEntryImpl
import bose.ankush.home.data.HomeSessionCleanerImpl
import bose.ankush.home.data.events.LocalFirstPlaceEventsRepository
import bose.ankush.home.data.events.NearbyPlaceEventSource
import bose.ankush.home.data.events.PlaceEventWriteLock
import bose.ankush.home.data.events.sync.PlaceEventSyncEngine
import bose.ankush.home.data.events.sync.PlaceEventSyncScheduler
import bose.ankush.home.data.repository.WeatherRepositoryImpl
import bose.ankush.home.domain.ai.SummarizeWeather
import bose.ankush.home.domain.ai.WeatherPromptBuilder
import bose.ankush.home.domain.ai.WeatherRules
import bose.ankush.home.domain.repository.PlaceEventsRepository
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.home.domain.usecase.CreateNearbyEvent
import bose.ankush.home.domain.usecase.DeletePlaceEvent
import bose.ankush.home.domain.usecase.DiscardFailedPlaceEvent
import bose.ankush.home.domain.usecase.FindNearestSavedPlace
import bose.ankush.home.domain.usecase.GetActiveCoordinates
import bose.ankush.home.domain.usecase.GetActiveCoordinatesImpl
import bose.ankush.home.domain.usecase.GetAirQuality
import bose.ankush.home.domain.usecase.GetNearbyEvents
import bose.ankush.home.domain.usecase.GetWeatherReport
import bose.ankush.home.domain.usecase.ObserveAccountPhotoUrl
import bose.ankush.home.domain.usecase.ObservePlaceEvents
import bose.ankush.home.domain.usecase.RefreshAccount
import bose.ankush.home.domain.usecase.RefreshWeatherReport
import bose.ankush.home.domain.usecase.UpdatePlaceEvent
import bose.ankush.home.presentation.HomeViewModel
import bose.ankush.home.presentation.account.AccountAvatarViewModel
import bose.ankush.home.presentation.ai.AiSummaryViewModel
import bose.ankush.home.presentation.nearby.NearbyViewModel
import bose.ankush.home.presentation.places.SavedPlacesViewModel
import bose.ankush.tokenstorage.api.TokenStorage
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

/** Platform-specific bindings: [bose.ankush.home.domain.location.LocationClient],
 * [bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate]. */
expect val homePlatformModule: Module

/**
 * Offline-first place events: Room store, shared sync engine, repository and use cases.
 * Declared before [homeDomainModule], which includes it, so it is initialised first. The
 * [PlaceEventSyncScheduler] (also bound as [bose.ankush.home.PlaceEventSyncTrigger]) comes from
 * [homePlatformModule]: WorkManager on Android, in-process foreground sync on iOS.
 */
private val placeEventsModule: Module =
    module {
        single { PlaceEventWriteLock() }
        single { PlaceEventSyncEngine(get(), get(), get()) }
        single<PlaceEventsRepository> { LocalFirstPlaceEventsRepository(get(), get(), get()) }
        single { NearbyPlaceEventSource(get(), get()) }
        factory { ObservePlaceEvents(get()) }
        factory { UpdatePlaceEvent(get()) }
        factory { DeletePlaceEvent(get()) }
        factory { DiscardFailedPlaceEvent(get()) }
    }

/** Whether a place-event sync may run: a JWT is stored. Shared by both platform schedulers. */
internal fun Scope.placeEventSyncSignedIn(): suspend () -> Boolean {
    val tokens = get<TokenStorage>()
    return { !tokens.getToken().isNullOrBlank() }
}

/** Relies on `storageDomainModule` (module `:storage`) being loaded for
 * `bose.ankush.storage.api.LocationPreferencesStorage`. */
val homeDomainModule: Module =
    module {
        single<WeatherRepository> { WeatherRepositoryImpl(get(), get(), get(), get()) }
        factory { GetWeatherReport(get()) }
        factory { RefreshWeatherReport(get()) }
        factory { GetAirQuality(get()) }
        factory { GetNearbyEvents(get<NearbyPlaceEventSource>()) }
        factory { FindNearestSavedPlace(get()) }
        factory { CreateNearbyEvent(get<NearbyPlaceEventSource>()) }
        factory { ObserveAccountPhotoUrl(get()) }
        factory { RefreshAccount(get()) }
        factory { SummarizeWeather(get(), WeatherRules(), WeatherPromptBuilder()) }
        single<HomeLocationCoordinator> { HomeLocationCoordinatorImpl(get()) }
        single { HomeSavedPlacesEntryImpl() } bind HomeSavedPlacesEntry::class
        single<HomeSessionCleaner> { HomeSessionCleanerImpl(get(), get(), get(), get()) }
        factory<GetActiveCoordinates> { GetActiveCoordinatesImpl(get(), get()) }
        includes(placeEventsModule)
    }

val homeViewModelModule: Module =
    module {
        viewModelOf(::HomeViewModel)
        viewModelOf(::NearbyViewModel)
        viewModelOf(::AccountAvatarViewModel)
        viewModelOf(::SavedPlacesViewModel)
        viewModel { AiSummaryViewModel(get(), get()) }
    }
