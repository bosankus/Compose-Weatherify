package bose.ankush.home.presentation

import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.finder.domain.model.Location
import bose.ankush.home.domain.location.ActiveCoordinates
import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.model.RefreshOutcome
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.domain.model.WeatherSnapshot
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.home.domain.usecase.GetActiveCoordinatesImpl
import bose.ankush.home.domain.usecase.ObserveHomeWeather
import bose.ankush.home.presentation.places.FakeFinder
import bose.ankush.home.presentation.places.FakeLocationPreferences
import bose.ankush.home.presentation.places.NoopAnalytics
import bose.ankush.home.presentation.places.SavedPlacesIntent
import bose.ankush.home.presentation.places.placesViewModel
import bose.ankush.home.presentation.places.suggestion
import bose.ankush.storage.model.LocationPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Saving or picking a place on the saved places page writes the location override, and
 * [HomeViewModel] refetches the forecast for those coordinates without any direct call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelLocationSwitchTest {
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun savingAPlaceRefetchesHomeForItsCoordinates() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val preferences = FakeLocationPreferences(LocationPreferences(latitude = GPS_LAT, longitude = GPS_LON))
            val repository = RecordingWeatherRepository()
            val home = homeViewModel(repository, preferences)
            val places = placesViewModel(finder = FakeFinder(), preferences = preferences)
            advanceUntilIdle()
            assertEquals(listOf(GPS to false), repository.refreshes)

            places.processIntent(SavedPlacesIntent.OpenSearch)
            places.processIntent(
                SavedPlacesIntent.SaveSuggestion(
                    suggestion(
                        "Paris",
                        PARIS_LAT,
                        PARIS_LON,
                    ),
                ),
            )
            advanceUntilIdle()

            assertEquals(PARIS to true, repository.refreshes.last())
            val state = home.state.value
            assertEquals(PARIS_POINT, state.place?.point)
            assertTrue(state.place?.isPinned == true)
            assertEquals("Paris", state.pinnedPlaceName)
        }

    @Test
    fun switchKeepsTheOldPlaceUntilTheNewForecastLands() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val preferences = FakeLocationPreferences(LocationPreferences(latitude = GPS_LAT, longitude = GPS_LON))
            val repository = RecordingWeatherRepository()
            val home = homeViewModel(repository, preferences)
            val places = placesViewModel(finder = FakeFinder(mutableListOf(PARIS_PLACE)), preferences = preferences)
            advanceUntilIdle()

            val release = CompletableDeferred<Unit>()
            repository.gate = release
            places.processIntent(SavedPlacesIntent.Select(PARIS_PLACE))
            advanceUntilIdle()

            assertEquals(PARIS to true, repository.refreshes.last())
            assertEquals(GPS_POINT, home.state.value.place?.point)
            assertFalse(home.state.value.place?.isPinned == true)
            assertTrue(home.state.value.isLoading)

            release.complete(Unit)
            advanceUntilIdle()
            assertEquals(PARIS_POINT, home.state.value.place?.point)
            assertTrue(home.state.value.place?.isPinned == true)
            assertFalse(home.state.value.isLoading)
        }

    /** Room still holds the GPS forecast, so a failed switch must not label it Paris. */
    @Test
    fun failedSwitchKeepsTheOldPlaceAndReportsOffline() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val preferences = FakeLocationPreferences(
                LocationPreferences(
                    latitude = GPS_LAT,
                    longitude = GPS_LON
                )
            )
            val repository = RecordingWeatherRepository()
            val home = homeViewModel(repository, preferences)
            val places = placesViewModel(
                finder = FakeFinder(mutableListOf(PARIS_PLACE)),
                preferences = preferences
            )
            advanceUntilIdle()

            // Its message is used as-is; other errors read string resources, absent in host tests.
            repository.failure = LocationClient.LocationException("offline")
            places.processIntent(SavedPlacesIntent.Select(PARIS_PLACE))
            advanceUntilIdle()

            val state = home.state.value
            assertEquals(PARIS to true, repository.refreshes.last())
            assertEquals(GPS_POINT, state.place?.point)
            assertFalse(state.place?.isPinned == true)
            assertTrue(state.isOffline)
            assertEquals("offline", state.offlineMessage)
            assertFalse(state.isLoading)
        }

    @Test
    fun currentLocationRefetchesForGps() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val preferences = FakeLocationPreferences(LocationPreferences(latitude = GPS_LAT, longitude = GPS_LON))
            val repository = RecordingWeatherRepository()
            val home = homeViewModel(repository, preferences)
            val places = placesViewModel(finder = FakeFinder(mutableListOf(PARIS_PLACE)), preferences = preferences)
            advanceUntilIdle()
            places.processIntent(SavedPlacesIntent.Select(PARIS_PLACE))
            advanceUntilIdle()

            places.processIntent(SavedPlacesIntent.UseCurrentLocation)
            advanceUntilIdle()

            assertEquals(GPS to true, repository.refreshes.last())
            assertEquals(GPS_POINT, home.state.value.place?.point)
            assertFalse(home.state.value.place?.isPinned == true)
        }

    private fun homeViewModel(
        repository: RecordingWeatherRepository,
        preferences: FakeLocationPreferences,
    ): HomeViewModel {
        val coordinates =
            GetActiveCoordinatesImpl(
                client =
                    object : LocationClient {
                        override suspend fun getCurrentLocation(): Result<Coordinates> =
                            Result.success(Coordinates(GPS_LAT, GPS_LON))

                        override fun hasLocationPermission(): Boolean = true
                    },
                storage = preferences,
            )
        return HomeViewModel(
            observeHomeWeather = ObserveHomeWeather(repository, coordinates),
            getActiveCoordinates = coordinates,
            remoteConfigGate =
                object : HomeRemoteConfigGate {
                    override fun initialize(onActivated: () -> Unit) = Unit

                    override fun isNotificationBannerEnabled(): Boolean = false
                },
            analyticsTracker = NoopAnalytics as AnalyticsTracker,
            errorReporter = RecordingErrorReporter(),
            // Resource strings can't load in host tests; the failure's own text stands in.
            errorMessage = { it.message },
        )
    }

    /** Room's single forecast row; each refresh writes a forecast tagged with its coordinates. */
    private class RecordingWeatherRepository : WeatherRepository {
        private val weather = MutableStateFlow<WeatherSnapshot?>(
            WeatherSnapshot(
                forecast(id = 0L),
                airQuality = null
            )
        )
        val refreshes = mutableListOf<Pair<Pair<Double, Double>, Boolean>>()
        var gate: CompletableDeferred<Unit>? = null
        var failure: Throwable? = null

        override val snapshot: Flow<WeatherSnapshot?> = weather

        override suspend fun refresh(
            place: ActiveCoordinates,
            forceRefresh: Boolean,
        ): Result<RefreshOutcome> {
            refreshes += (place.coordinates.latitude to place.coordinates.longitude) to forceRefresh
            gate?.await()
            failure?.let { return Result.failure(it) }
            weather.value =
                WeatherSnapshot(
                    forecast(id = refreshes.size.toLong()),
                    airQuality = null,
                    point = GeoPoint(place.coordinates.latitude, place.coordinates.longitude),
                    placeName = place.overrideName,
                )
            return Result.success(RefreshOutcome.Updated)
        }

        override suspend fun clearAllData() = Unit
    }

    private companion object {
        const val GPS_LAT = 22.57
        const val GPS_LON = 88.36
        const val PARIS_LAT = 48.8566
        const val PARIS_LON = 2.3522
        val GPS = GPS_LAT to GPS_LON
        val PARIS = PARIS_LAT to PARIS_LON
        val GPS_POINT = GeoPoint(GPS_LAT, GPS_LON)
        val PARIS_POINT = GeoPoint(PARIS_LAT, PARIS_LON)
        val PARIS_PLACE = Location(id = "paris", name = "Paris", lat = PARIS_LAT, lon = PARIS_LON)

        fun forecast(id: Long): WeatherForecast =
            WeatherForecast(
                id = id,
                current =
                    WeatherForecast.Current(
                        clouds = 1,
                        dt = 1L,
                        feels_like = 20.0,
                        humidity = 10,
                        pressure = 1000,
                        sunrise = 1L,
                        sunset = 2L,
                        temp = 21.0,
                        uvi = 0.0,
                        weather = emptyList(),
                        wind_gust = null,
                        wind_speed = 1.0,
                    ),
            )
    }
}
