package bose.ankush.home.presentation

import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
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
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.model.LocationPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The saved Room forecast has to reach [HomeViewModel.state] before [WeatherRepository.refresh]
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
            assertEquals(GeoPoint(SAVED_LAT, SAVED_LON), viewModel.state.value.place?.point)
            // The forecast is on screen and the loader runs over it.
            assertTrue(viewModel.state.value.isLoading)
            assertEquals(1, repository.refreshCallCount)
            assertFalse(releaseRefresh.isCompleted)
            releaseRefresh.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun loadingIsTrueOnlyWhileTheRefreshRuns() =
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
            assertTrue(viewModel.state.value.isLoading)
            releaseRefresh.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isLoading)
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
            // Waiting on GPS: the loader runs, but over the forecast, not instead of it.
            assertTrue(state.isLoading)
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
            assertTrue(viewModel.state.value.isLoading)
            assertTrue(viewModel.state.value.hasCheckedCache)
            assertNull(viewModel.state.value.place)
            assertEquals(0, repository.refreshCallCount)

            preferencesGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(GeoPoint(SAVED_LAT, SAVED_LON), viewModel.state.value.place?.point)
            assertEquals(
                CACHED_ID,
                viewModel.state.value.weatherData
                    ?.id,
            )
            assertEquals(1, repository.refreshCallCount)
            assertTrue(viewModel.state.value.isLoading)
            releaseRefresh.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isLoading)
        }

    /** A failed refresh used to be dropped in the repository; now the cached forecast says so. */
    @Test
    fun failedRefreshKeepsCachedForecastAndReportsOffline() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeWeatherRepository(
                    initial = cachedForecast(),
                    onRefresh = {},
                    failure = REFRESH_FAILURE,
                )
            val viewModel = homeViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(CACHED_ID, state.weatherData?.id)
            assertTrue(state.isOffline)
            assertEquals(REFRESH_FAILURE.message, state.offlineMessage)
            assertNull(state.error)
            assertFalse(state.isLoading)
        }

    @Test
    fun failedAttemptIsReportedOnce() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val reporter = RecordingErrorReporter()
            val repository = FakeWeatherRepository(
                initial = cachedForecast(),
                onRefresh = {},
                failure = REFRESH_FAILURE
            )
            homeViewModel(repository, errorReporter = reporter)
            advanceUntilIdle()

            val (reported, context) = reporter.recorded.single()
            assertSame(REFRESH_FAILURE, reported)
            // Not a NetworkException, so there is no HTTP or network code to attach.
            assertEquals(mapOf("errorCode" to "none"), context)
        }

    @Test
    fun failedRefreshWithEmptyCacheShowsError() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeWeatherRepository(
                    initial = null,
                    onRefresh = {},
                    failure = REFRESH_FAILURE,
                )
            val viewModel = homeViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertNull(state.weatherData)
            assertEquals(REFRESH_FAILURE.message, state.error)
            assertFalse(state.isLoading)
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
        errorReporter: RecordingErrorReporter = RecordingErrorReporter(),
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
        val coordinates =
            GetActiveCoordinatesImpl(
                client =
                    object : LocationClient {
                        override suspend fun getCurrentLocation(): Result<Coordinates> = gpsFix()

                        override fun hasLocationPermission(): Boolean = true
                    },
                storage =
                    object : LocationPreferencesStorage {
                        override fun getLocationPreferencesFlow(): Flow<LocationPreferences> =
                            preferencesFlow

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
            )
        return HomeViewModel(
            observeHomeWeather = ObserveHomeWeather(repository, coordinates),
            getActiveCoordinates = coordinates,
            remoteConfigGate =
                object : HomeRemoteConfigGate {
                    override fun initialize(onActivated: () -> Unit) = Unit

                    override fun isNotificationBannerEnabled(): Boolean = false
                },
            analyticsTracker =
                object : AnalyticsTracker {
                    override fun track(event: AnalyticsEvent) = Unit
                },
            errorReporter = errorReporter,
            // Resource strings can't load in host tests; the failure's own text stands in.
            errorMessage = { it.message },
        )
    }

    private class FakeWeatherRepository(
        initial: WeatherForecast?,
        private val onRefresh: suspend () -> Unit,
        private val failure: Throwable? = null,
    ) : WeatherRepository {
        private val weather = MutableStateFlow(initial)
        var refreshCallCount = 0
            private set

        /** Rows without a point, as saved before schema 6: they take the saved spot as their place. */
        override val snapshot: Flow<WeatherSnapshot?> =
            weather.map { forecast -> forecast?.let { WeatherSnapshot(it, airQuality = null) } }

        override suspend fun refresh(
            place: ActiveCoordinates,
            forceRefresh: Boolean,
        ): Result<RefreshOutcome> {
            refreshCallCount++
            onRefresh()
            return failure?.let { Result.failure(it) } ?: Result.success(RefreshOutcome.Updated)
        }

        override suspend fun clearAllData() = Unit
    }

    private companion object {
        const val SAVED_LAT = 37.42
        const val SAVED_LON = -122.08
        const val CACHED_ID = 42L
        const val CACHED_UPDATED = 1_700_000_000_000L

        /** Its message is used as-is; other errors read string resources, absent in host tests. */
        val REFRESH_FAILURE = LocationClient.LocationException("offline")
    }
}
