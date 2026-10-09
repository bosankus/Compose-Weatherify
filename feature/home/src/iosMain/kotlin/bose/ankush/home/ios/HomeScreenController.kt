package bose.ankush.home.ios

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.presentation.HomeEffect
import bose.ankush.home.presentation.HomeIntent
import bose.ankush.home.presentation.HomeViewModel
import bose.ankush.home.presentation.account.AccountAvatarIntent
import bose.ankush.home.presentation.account.AccountAvatarViewModel
import bose.ankush.home.presentation.ai.AiSummaryIntent
import bose.ankush.home.presentation.ai.AiSummaryUiState
import bose.ankush.home.presentation.ai.AiSummaryViewModel
import bose.ankush.home.presentation.nearby.NearbyIntent
import bose.ankush.home.presentation.nearby.NearbyViewModel
import bose.ankush.home.presentation.places.SavedPlacesEffect
import bose.ankush.home.presentation.places.SavedPlacesIntent
import bose.ankush.home.presentation.places.SavedPlacesViewModel
import bose.ankush.home.presentation.screen.BackgroundPhoto
import bose.ankush.home.presentation.screen.SkyCondition
import bose.ankush.home.presentation.screen.skyCondition
import bose.ankush.home.presentation.screen.unsplashQuery
import bose.ankush.network.api.UnsplashApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * The SwiftUI home screen's single entry into shared code. It owns the same four ViewModels the
 * Android home binds ([HomeViewModel], [NearbyViewModel], [AccountAvatarViewModel],
 * [SavedPlacesViewModel]) in its own store, folds their state into one [HomeScreenUi], and turns
 * taps into their intents. Swift creates one per home screen and calls [close] when it goes away.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenController : KoinComponent {
    private val scope = MainScope()
    private val store = ViewModelStore()
    private val provider =
        ViewModelProvider.create(
            store,
            viewModelFactory {
                initializer { get<HomeViewModel>() }
                initializer { get<NearbyViewModel>() }
                initializer { get<AccountAvatarViewModel>() }
                initializer { get<SavedPlacesViewModel>() }
                initializer { get<AiSummaryViewModel>() }
            },
        )
    private val home = provider[HomeViewModel::class]
    private val nearby = provider[NearbyViewModel::class]
    private val avatar = provider[AccountAvatarViewModel::class]
    private val places = provider[SavedPlacesViewModel::class]
    private val aiSummary = provider[AiSummaryViewModel::class]
    private val geocoder = get<HomeGeocoder>()
    private val photos = IosBackdropPhotos(get<UnsplashApi>())

    /** The nearby events strip and the new-event sheet. */
    val nearbyActions = NearbyActions(nearby)

    private val strings = MutableStateFlow<HomeStrings?>(null)
    private val pickedSuggestion = MutableStateFlow<String?>(null)
    private val onPlacesPage = MutableStateFlow(false)

    /** The saved places page and the add-a-place sheet. */
    val placeActions = PlaceActions(places, pickedSuggestion, onPlacesPage)

    /** Reverse-geocoded name of the forecast coordinates, as on the Android header. */
    private val geocodedPlace: StateFlow<String?> =
        home.state
            .map { it.userLocation }
            .distinctUntilChanged()
            .mapLatest { location ->
                location?.let {
                    geocoder.reverseGeocode(
                        it.first,
                        it.second
                    )
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, null)

    /** The weather summary sheet. Stays hidden until the Swift on-device client reports it is usable. */
    val aiSummaryAvailable: StateFlow<Boolean> =
        combine(
            aiSummary.state.map { it.isAvailable },
            home.state.map { it.weatherData != null }) { available, hasForecast ->
            available && hasForecast
        }.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, false)

    val aiSummaryState: StateFlow<AiSummaryUiState> =
        aiSummary.state.map { it.summary }
            .stateIn(scope, SharingStarted.Eagerly, AiSummaryUiState())

    fun openAiSummary() {
        val state = home.state.value
        val forecast = state.weatherData ?: return
        val place = state.activeLocationName?.takeIf { it.isNotBlank() } ?: geocodedPlace.value
        aiSummary.onIntent(AiSummaryIntent.Open(forecast, place))
    }

    fun retryAiSummary() = aiSummary.onIntent(AiSummaryIntent.Retry)

    fun closeAiSummary() = aiSummary.onIntent(AiSummaryIntent.Dismiss)

    /** The photo behind the screen right now, kept whole for the Unsplash download ping. */
    private var shownPhoto: BackgroundPhoto? = null

    private val backdrop: Flow<BackdropUi> =
        home.state
            .map { state ->
                val current = state.weatherData?.current
                val main =
                    current
                        ?.weather
                        ?.firstOrNull()
                        ?.main
                        ?.takeIf { it.isNotBlank() }
                (current?.skyCondition() ?: SkyCondition.CLOUDS) to main?.let(::unsplashQuery)
            }.distinctUntilChanged()
            .flatMapLatest { (condition, query) ->
                photos.photos(condition, query).map { photo ->
                    shownPhoto = photo
                    BackdropUi(condition, photo?.toUi())
                }
            }.onStart { emit(BackdropUi(SkyCondition.CLOUDS, null)) }

    private val homeSide =
        combine(
            home.state,
            home.refreshing,
            geocodedPlace,
            backdrop
        ) { state, refreshing, place, backdrop ->
            HomeSide(state, refreshing, place, backdrop)
        }

    private val featureSide =
        combine(
            nearby.state,
            avatar.state,
            places.state,
            pickedSuggestion,
            onPlacesPage
        ) { n, a, p, pick, page ->
            FeatureSide(n, a.photoUrl, p, pick, page)
        }

    /** Null until the copy is loaded; Swift shows the bare backdrop until then. */
    val state: StateFlow<HomeScreenUi?> =
        combine(strings.filterNotNull(), homeSide, featureSide) { copy, homeSide, featureSide ->
            buildHomeScreenUi(copy, homeSide, featureSide)
        }.stateIn(scope, SharingStarted.Eagerly, null)

    /** Permission prompts, settings links, and page jumps for Swift to carry out. */
    val events: Flow<HomeScreenEvent> =
        merge(
            home.effect.map { effect ->
                when (effect) {
                    HomeEffect.RequestLocationPermission -> HomeScreenEvent.RequestLocationPermission
                    HomeEffect.RequestNotificationPermission -> HomeScreenEvent.RequestNotificationPermission
                    HomeEffect.OpenNotificationSettings -> HomeScreenEvent.OpenNotificationSettings
                    HomeEffect.RequestGpsPermission -> HomeScreenEvent.OpenLocationSettings
                }
            },
            places.effect.map { effect ->
                when (effect) {
                    SavedPlacesEffect.ShowWeather -> HomeScreenEvent.ShowWeather
                    SavedPlacesEffect.ShowPlaces -> HomeScreenEvent.ShowPlaces
                }
            },
        )

    init {
        scope.launch { strings.value = loadHomeStrings() }
        avatar.onIntent(AccountAvatarIntent.Refresh)
        scope.launch {
            home.state
                .map { it.userLocation }
                .distinctUntilChanged()
                .collect { location ->
                    location?.let {
                        nearby.onIntent(
                            NearbyIntent.LocationChanged(
                                GeoPoint(
                                    it.first,
                                    it.second
                                )
                            )
                        )
                    }
                }
        }
        scope.launch {
            combine(home.state.map { it.activeLocationName }, geocodedPlace) { active, geocoded ->
                active?.takeIf { it.isNotBlank() } ?: geocoded
            }.distinctUntilChanged().collect { name ->
                if (!name.isNullOrBlank()) nearby.onIntent(NearbyIntent.PlaceNameChanged(name))
            }
        }
    }

    /** The app language changed: reload the fixed copy (per-item copy follows on the next frame). */
    fun reloadStrings() {
        scope.launch { strings.value = loadHomeStrings() }
    }

    /** The screen came back to the foreground; the account photo URL is short-lived. */
    fun onAppear() = avatar.onIntent(AccountAvatarIntent.Refresh)

    /** Pull to refresh. Returns once the refresh settles, so the native spinner tracks it. */
    suspend fun refresh() {
        home.processIntent(HomeIntent.Refresh)
        if (onPlacesPage.value) places.processIntent(SavedPlacesIntent.Load)
        withTimeoutOrNull(REFRESH_START_TIMEOUT_MS) { home.state.first { it.isRefreshing } }
        withTimeoutOrNull(REFRESH_END_TIMEOUT_MS) { home.state.first { !it.isRefreshing } }
    }

    fun retryForecast() = home.processIntent(HomeIntent.FetchLocation)

    fun resetLocation() = home.processIntent(HomeIntent.ResetLocationOverride)

    fun requestLocationPermission() = home.processIntent(HomeIntent.RequestLocationPermission)

    /** Location was just granted (or was already): fetch for the device position. */
    fun locationPermissionGranted() = home.processIntent(HomeIntent.FetchLocation)

    fun notificationPermissionChanged(granted: Boolean) =
        home.processIntent(HomeIntent.UpdateNotificationPermissionState(granted))

    fun notificationPermissionResult(
        granted: Boolean,
        permanentlyDeclined: Boolean,
    ) = home.processIntent(HomeIntent.NotificationPermissionResult(granted, permanentlyDeclined))

    fun enableNotifications() = home.processIntent(HomeIntent.EnableNotificationBanner)

    fun dismissNotifications() = home.processIntent(HomeIntent.DismissNotificationBanner)

    /** Call once the backdrop photo is actually on screen. */
    fun backdropPhotoShown() {
        val photo = shownPhoto?.takeIf { it.downloadLocation.isNotBlank() } ?: return
        scope.launch { photos.trackShown(photo) }
    }

    /** Stops every flow and clears the ViewModels. The controller is unusable afterwards. */
    fun close() {
        scope.cancel()
        store.clear()
    }

    private companion object {
        const val REFRESH_START_TIMEOUT_MS = 1_000L
        const val REFRESH_END_TIMEOUT_MS = 30_000L
    }
}
