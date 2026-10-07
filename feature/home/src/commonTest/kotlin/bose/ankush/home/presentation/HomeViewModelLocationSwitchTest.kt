package bose.ankush.home.presentation

import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.usecase.GetSavedLocationsUseCase
import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.home.domain.usecase.GetAirQuality
import bose.ankush.home.domain.usecase.GetWeatherReport
import bose.ankush.home.domain.usecase.RefreshWeatherReport
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
                        PARIS_LON
                    )
                )
            )
            advanceUntilIdle()

            assertEquals(PARIS to true, repository.refreshes.last())
            val state = home.state.value
            assertEquals(PARIS, state.userLocation)
            assertTrue(state.isLocationOverridden)
            assertEquals("Paris", state.activeLocationName)
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
            assertEquals(GPS, home.state.value.userLocation)
            assertFalse(home.state.value.isLocationOverridden)
            assertTrue(home.state.value.isRefreshing)

            release.complete(Unit)
            advanceUntilIdle()
            assertEquals(PARIS, home.state.value.userLocation)
            assertTrue(home.state.value.isLocationOverridden)
            assertFalse(home.state.value.isRefreshing)
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
            assertEquals(GPS, home.state.value.userLocation)
            assertFalse(home.state.value.isLocationOverridden)
        }

    private fun homeViewModel(
        repository: RecordingWeatherRepository,
        preferences: FakeLocationPreferences,
    ): HomeViewModel =
        HomeViewModel(
            refreshWeatherReport = RefreshWeatherReport(repository),
            getWeatherReport = GetWeatherReport(repository),
            getAirQuality = GetAirQuality(repository),
            locationClient =
                object : LocationClient {
                    override suspend fun getCurrentLocation(): Result<Coordinates> =
                        Result.success(Coordinates(GPS_LAT, GPS_LON))

                    override fun hasLocationPermission(): Boolean = true
                },
            locationPreferencesStorage = preferences,
            remoteConfigGate =
                object : HomeRemoteConfigGate {
                    override fun initialize(onActivated: () -> Unit) = Unit

                    override fun isNotificationBannerEnabled(): Boolean = false

                    override fun isLeaveByFakeDoorEnabled(): Boolean = false
                },
            analyticsTracker = NoopAnalytics as AnalyticsTracker,
            getSavedLocationsUseCase =
                object : GetSavedLocationsUseCase {
                    override suspend fun invoke(): Result<List<Location>> = Result.success(emptyList())
                },
        )

    /** Room's single forecast row; each refresh writes a forecast tagged with its coordinates. */
    private class RecordingWeatherRepository : WeatherRepository {
        private val weather = MutableStateFlow<WeatherForecast?>(forecast(id = 0L))
        val refreshes = mutableListOf<Pair<Pair<Double, Double>, Boolean>>()
        var gate: CompletableDeferred<Unit>? = null

        override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality> =
            MutableStateFlow(AirQuality())

        override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?> = weather

        override suspend fun refreshWeatherData(
            coordinates: Pair<Double, Double>,
            forceRefresh: Boolean,
        ) {
            refreshes += coordinates to forceRefresh
            gate?.await()
            weather.value = forecast(id = refreshes.size.toLong())
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
