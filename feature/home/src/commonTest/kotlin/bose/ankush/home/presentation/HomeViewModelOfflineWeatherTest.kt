package bose.ankush.home.presentation

import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.home.domain.usecase.GetActiveCoordinatesImpl
import bose.ankush.home.domain.usecase.GetAirQuality
import bose.ankush.home.domain.usecase.GetWeatherReport
import bose.ankush.home.domain.usecase.RefreshWeatherReport
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.model.LocationPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The saved Room forecast has to reach [HomeViewModel.state] before [WeatherRepository.refreshWeatherData]
 * runs. An empty cache stays on the loading state instead of being replaced with a blank success.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelOfflineWeatherTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun cachedForecastIsEmittedBeforeNetworkRefresh() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val cached =
                WeatherForecast(
                    id = CACHED_ID,
                    lastUpdated = CACHED_UPDATED,
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
            val releaseRefresh = CompletableDeferred<Unit>()
            var weatherWhenRefreshStarted: WeatherForecast? = null
            lateinit var viewModel: HomeViewModel
            val repository =
                FakeWeatherRepository(
                    initial = cached,
                    onRefresh = {
                        weatherWhenRefreshStarted = viewModel.state.value.weatherData
                        releaseRefresh.await()
                    },
                )
            viewModel = homeViewModel(repository)
            advanceUntilIdle()

            assertEquals(CACHED_ID, weatherWhenRefreshStarted?.id)
            assertEquals(CACHED_UPDATED, weatherWhenRefreshStarted?.lastUpdated)
            assertEquals(21.0, weatherWhenRefreshStarted?.current?.temp)
            assertEquals(
                CACHED_ID,
                viewModel.state.value.weatherData
                    ?.id,
            )
            assertEquals(SAVED_LAT to SAVED_LON, viewModel.state.value.userLocation)
            assertFalse(viewModel.state.value.isLoading)
            assertEquals(1, repository.refreshCallCount)
            assertFalse(releaseRefresh.isCompleted)
            assertTrue(viewModel.refreshing.value)
            releaseRefresh.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.refreshing.value)
        }

    @Test
    fun refreshingIsTrueOnlyWhileNetworkRefreshRuns() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val releaseRefresh = CompletableDeferred<Unit>()
            val repository =
                FakeWeatherRepository(
                    initial =
                        WeatherForecast(
                            id = CACHED_ID,
                            lastUpdated = CACHED_UPDATED,
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
                        ),
                    onRefresh = { releaseRefresh.await() },
                )
            val viewModel = homeViewModel(repository)
            advanceUntilIdle()
            assertTrue(viewModel.refreshing.value)
            assertFalse(viewModel.state.value.isRefreshing)
            assertFalse(viewModel.state.value.isLoading)
            releaseRefresh.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.refreshing.value)
        }

    @Test
    fun emptyCacheStaysLoadingUntilRefreshSettles() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val releaseRefresh = CompletableDeferred<Unit>()
            var refreshStarted = false
            val repository =
                FakeWeatherRepository(
                    initial = null,
                    onRefresh = {
                        refreshStarted = true
                        releaseRefresh.await()
                    },
                )
            val viewModel = homeViewModel(repository)
            advanceUntilIdle()

            assertTrue(refreshStarted)
            assertNull(viewModel.state.value.weatherData)
            assertTrue(viewModel.state.value.isLoading)
            assertTrue(viewModel.state.value.hasCheckedCache)
            releaseRefresh.complete(Unit)
            advanceUntilIdle()
        }

    /**
     * Reload with Room populated but no saved coordinates and a GPS fix that has not
     * arrived. The cached forecast must be on screen, not the loading page.
     */
    @Test
    fun reloadShowsCachedForecastWithoutSavedCoordinatesOrGps() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val gpsFix = CompletableDeferred<Result<Coordinates>>()
            val repository = FakeWeatherRepository(initial = cachedForecast(), onRefresh = {})
            val viewModel =
                homeViewModel(
                    repository = repository,
                    savedPreferences = LocationPreferences(),
                    gpsFix = { gpsFix.await() },
                )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(CACHED_ID, state.weatherData?.id)
            assertFalse(state.isLoading)
            assertTrue(state.hasCheckedCache)
            assertEquals(0, repository.refreshCallCount)
            gpsFix.complete(Result.success(Coordinates(SAVED_LAT, SAVED_LON)))
            advanceUntilIdle()
        }

    /** Reload where the saved-coordinate read is slow. Room still paints first. */
    @Test
    fun reloadPaintsCacheBeforeSavedCoordinatesLoad() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val preferencesGate = CompletableDeferred<Unit>()
            val releaseRefresh = CompletableDeferred<Unit>()
            val repository =
                FakeWeatherRepository(initial = cachedForecast(), onRefresh = { releaseRefresh.await() })
            val viewModel = homeViewModel(repository = repository, preferencesGate = preferencesGate)
            advanceUntilIdle()

            assertEquals(
                CACHED_ID,
                viewModel.state.value.weatherData
                    ?.id,
            )
            assertFalse(viewModel.state.value.isLoading)
            assertTrue(viewModel.state.value.hasCheckedCache)
            assertNull(viewModel.state.value.userLocation)
            assertFalse(viewModel.refreshing.value)

            preferencesGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(SAVED_LAT to SAVED_LON, viewModel.state.value.userLocation)
            assertEquals(
                CACHED_ID,
                viewModel.state.value.weatherData
                    ?.id,
            )
            assertTrue(viewModel.refreshing.value)
            assertFalse(viewModel.state.value.isLoading)
            releaseRefresh.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.refreshing.value)
        }

    private fun cachedForecast(): WeatherForecast =
        WeatherForecast(
            id = CACHED_ID,
            lastUpdated = CACHED_UPDATED,
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

    private fun homeViewModel(
        repository: FakeWeatherRepository,
        savedPreferences: LocationPreferences = LocationPreferences(latitude = SAVED_LAT, longitude = SAVED_LON),
        preferencesGate: CompletableDeferred<Unit>? = null,
        gpsFix: suspend () -> Result<Coordinates> = { Result.success(Coordinates(SAVED_LAT, SAVED_LON)) },
    ): HomeViewModel {
        val preferences = MutableStateFlow(savedPreferences)
        val preferencesFlow: Flow<LocationPreferences> =
            if (preferencesGate == null) {
                preferences
            } else {
                flow {
                    preferencesGate.await()
                    emitAll(preferences)
                }
            }
        return HomeViewModel(
            refreshWeatherReport = RefreshWeatherReport(repository),
            getWeatherReport = GetWeatherReport(repository),
            getAirQuality = GetAirQuality(repository),
            getActiveCoordinates =
                GetActiveCoordinatesImpl(
                    client =
                object : LocationClient {
                    override suspend fun getCurrentLocation(): Result<Coordinates> = gpsFix()

                    override fun hasLocationPermission(): Boolean = true
                },
                    storage =
                object : LocationPreferencesStorage {
                    override fun getLocationPreferencesFlow(): Flow<LocationPreferences> = preferencesFlow

                    override suspend fun saveLocationPreferences(coordinates: Pair<Double, Double>) {
                        preferences.value =
                            preferences.value.copy(
                                latitude = coordinates.first,
                                longitude = coordinates.second,
                            )
                    }

                    override suspend fun saveLocationOverride(
                        lat: Double,
                        lon: Double,
                        name: String,
                    ) = Unit

                    override suspend fun clearLocationOverride() = Unit

                    override suspend fun clearAll() = Unit
                },
                ),
            remoteConfigGate =
                object : HomeRemoteConfigGate {
                    override fun initialize(onActivated: () -> Unit) = Unit

                    override fun isNotificationBannerEnabled(): Boolean = false
                },
            analyticsTracker =
                object : AnalyticsTracker {
                    override fun track(event: AnalyticsEvent) = Unit
                },
        )
    }

    private class FakeWeatherRepository(
        initial: WeatherForecast?,
        private val onRefresh: suspend () -> Unit,
    ) : WeatherRepository {
        private val weather = MutableStateFlow(initial)
        var refreshCallCount = 0
            private set

        override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality> =
            MutableStateFlow(AirQuality())

        override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?> = weather

        override suspend fun refreshWeatherData(
            coordinates: Pair<Double, Double>,
            forceRefresh: Boolean,
        ) {
            refreshCallCount++
            onRefresh()
        }

        override suspend fun clearAllData() = Unit
    }

    private companion object {
        const val SAVED_LAT = 37.42
        const val SAVED_LON = -122.08
        const val CACHED_ID = 42L
        const val CACHED_UPDATED = 1_700_000_000_000L
    }
}
