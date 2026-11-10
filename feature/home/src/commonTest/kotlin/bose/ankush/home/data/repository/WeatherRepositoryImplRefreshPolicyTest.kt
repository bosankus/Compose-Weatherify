package bose.ankush.home.data.repository

import bose.ankush.home.domain.location.ActiveCoordinates
import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.model.RefreshOutcome
import bose.ankush.home.domain.repository.WeatherWearSync
import bose.ankush.network.repository.WeatherRemoteSource
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.model.AirQualityData
import bose.ankush.storage.model.StoredWeather
import bose.ankush.storage.model.WeatherData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import bose.ankush.network.model.WeatherForecast as NetworkWeatherForecast

/**
 * Exercises the cache/network refresh gate inside [WeatherRepositoryImpl]: the network is hit
 * when [forceRefresh] is true, or the saved row is not this place's, or it is over an hour old.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplRefreshPolicyTest {
    @Test
    fun refresh_skipsNetworkWhenThePlacesRowIsFresh() =
        runTest {
            val network = FakeWeatherRemoteSource()
            val storage = FakeWeatherStorage(savedRow(ageMs = THIRTY_MINUTES_MS))
            val repo = createRepository(network, storage)

            val result = repo.refresh(GPS, forceRefresh = false)

            assertEquals(RefreshOutcome.UpToDate, result.getOrThrow())
            assertEquals(0, network.refreshCallCount)
            assertEquals(0, storage.saveWeatherDataCallCount)
        }

    @Test
    fun refresh_hitsNetworkWhenStaleAndSavesThePlace() =
        runTest {
            val network = FakeWeatherRemoteSource()
            val storage = FakeWeatherStorage(savedRow(ageMs = TWO_HOURS_MS))
            val wearSync = FakeWeatherWearSync()
            val geocoder = FakeHomeGeocoder(name = "Test City")
            val repo = createRepository(network, storage, wearSync, geocoder)

            val result = repo.refresh(GPS, forceRefresh = false)

            assertEquals(RefreshOutcome.Updated, result.getOrThrow())
            assertEquals(1, network.refreshCallCount)
            assertEquals(1, storage.saveWeatherDataCallCount)
            val saved = storage.saved?.weather
            assertEquals(LAT, saved?.latitude)
            assertEquals(LON, saved?.longitude)
            assertEquals("Test City", saved?.placeName)
            assertEquals(1, wearSync.syncCallCount)
            assertEquals("Test City", wearSync.lastLocationName)
        }

    @Test
    fun refresh_forceRefreshHitsNetworkEvenWhenFresh() =
        runTest {
            val network = FakeWeatherRemoteSource()
            val storage = FakeWeatherStorage(savedRow(ageMs = FIVE_MINUTES_MS))
            val repo = createRepository(network, storage)

            repo.refresh(GPS, forceRefresh = true)

            assertEquals(1, network.refreshCallCount)
            assertEquals(1, storage.saveWeatherDataCallCount)
        }

    @Test
    fun refresh_treatsAnEmptyCacheAsStale() =
        runTest {
            val network = FakeWeatherRemoteSource()
            val storage = FakeWeatherStorage(saved = null)
            val repo = createRepository(network, storage)

            repo.refresh(GPS, forceRefresh = false)

            assertEquals(1, network.refreshCallCount)
            assertEquals(1, storage.saveWeatherDataCallCount)
        }

    /** The bug a coordinate-less row hid: a fresh row for somewhere else was served as current. */
    @Test
    fun refresh_freshRowForAnotherPlaceIsStale() =
        runTest {
            val network = FakeWeatherRemoteSource()
            val farAway = savedRow(ageMs = FIVE_MINUTES_MS).copy(latitude = LAT + 1.0)
            val storage = FakeWeatherStorage(farAway)
            val repo = createRepository(network, storage)

            repo.refresh(GPS, forceRefresh = false)

            assertEquals(1, network.refreshCallCount)
        }

    @Test
    fun refresh_gpsJitterStillCountsAsTheSamePlace() =
        runTest {
            val network = FakeWeatherRemoteSource()
            // About 100 m north.
            val nearby = savedRow(ageMs = FIVE_MINUTES_MS).copy(latitude = LAT + 0.0009)
            val repo = createRepository(network, FakeWeatherStorage(nearby))

            val result = repo.refresh(GPS, forceRefresh = false)

            assertEquals(RefreshOutcome.UpToDate, result.getOrThrow())
            assertEquals(0, network.refreshCallCount)
        }

    @Test
    fun refresh_rowFromBeforeSchema6IsStale() =
        runTest {
            val network = FakeWeatherRemoteSource()
            val legacy = savedRow(ageMs = FIVE_MINUTES_MS).copy(latitude = null, longitude = null)
            val repo = createRepository(network, FakeWeatherStorage(legacy))

            repo.refresh(GPS, forceRefresh = false)

            assertEquals(1, network.refreshCallCount)
        }

    @Test
    fun refresh_pinnedPlaceSavesItsOwnNameWithoutGeocoding() =
        runTest {
            val storage = FakeWeatherStorage(saved = null)
            val wearSync = FakeWeatherWearSync()
            val geocoder = FakeHomeGeocoder(name = "Somewhere else")
            val repo = createRepository(FakeWeatherRemoteSource(), storage, wearSync, geocoder)

            repo.refresh(GPS.copy(isOverridden = true, overrideName = "Home"), forceRefresh = false)

            assertEquals("Home", storage.saved?.weather?.placeName)
            assertEquals("Home", wearSync.lastLocationName)
            assertEquals(0, geocoder.calls)
        }

    @Test
    fun refresh_networkFailureIsReturnedAndKeepsCache() =
        runTest {
            val network =
                FakeWeatherRemoteSource(result = Result.failure(IllegalStateException("offline")))
            val row = savedRow(ageMs = TWO_HOURS_MS)
            val storage = FakeWeatherStorage(row)
            val wearSync = FakeWeatherWearSync()
            val repo = createRepository(network, storage, wearSync)

            val result = repo.refresh(GPS, forceRefresh = false)

            assertEquals("offline", result.exceptionOrNull()?.message)
            assertEquals(1, network.refreshCallCount)
            assertEquals(0, storage.saveWeatherDataCallCount)
            assertEquals(0, wearSync.syncCallCount)
            assertEquals(row, storage.saved?.weather)
        }

    @Test
    fun refresh_unnamedPlaceSavesNoNameAndSyncsTheDefault() =
        runTest {
            val storage = FakeWeatherStorage(saved = null)
            val wearSync = FakeWeatherWearSync()
            val repo = createRepository(
                FakeWeatherRemoteSource(),
                storage,
                wearSync,
                FakeHomeGeocoder(name = null)
            )

            repo.refresh(GPS, forceRefresh = true)

            assertNull(storage.saved?.weather?.placeName)
            assertEquals("Current Location", wearSync.lastLocationName)
        }

    @Test
    fun refresh_failingGeocoderStillSaves() =
        runTest {
            val storage = FakeWeatherStorage(saved = null)
            val geocoder = FakeHomeGeocoder(failure = IllegalStateException("geocoder down"))
            val repo = createRepository(FakeWeatherRemoteSource(), storage, geocoder = geocoder)

            val result = repo.refresh(GPS, forceRefresh = true)

            assertEquals(RefreshOutcome.Updated, result.getOrThrow())
            assertNull(storage.saved?.weather?.placeName)
        }

    private fun savedRow(ageMs: Long) =
        WeatherData(
            id = 1L,
            lastUpdated = Clock.System.now().toEpochMilliseconds() - ageMs,
            latitude = LAT,
            longitude = LON,
        )

    private fun createRepository(
        network: FakeWeatherRemoteSource,
        storage: FakeWeatherStorage,
        wearSync: FakeWeatherWearSync = FakeWeatherWearSync(),
        geocoder: FakeHomeGeocoder = FakeHomeGeocoder(),
    ) = WeatherRepositoryImpl(
        weatherRemoteSource = network,
        weatherStorage = storage,
        weatherWearSync = wearSync,
        homeGeocoder = geocoder,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private class FakeWeatherRemoteSource(
        var result: Result<NetworkWeatherForecast> =
            Result.success(
                NetworkWeatherForecast(
                    data =
                        NetworkWeatherForecast.Data(
                            current = null,
                            daily = null,
                            hourly = null,
                            airQuality = null,
                        ),
                    message = "ok",
                    status = true,
                ),
            ),
    ) : WeatherRemoteSource {
        var refreshCallCount = 0
            private set

        override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>): Result<NetworkWeatherForecast> {
            refreshCallCount++
            assertEquals(COORDINATES, coordinates)
            return result
        }
    }

    private class FakeWeatherStorage(
        saved: WeatherData?,
    ) : WeatherStorage {
        private val stored = MutableStateFlow(saved?.let { StoredWeather(it, airQuality = null) })

        val saved: StoredWeather?
            get() = stored.value

        var saveWeatherDataCallCount = 0
            private set

        override fun observeWeather(): Flow<StoredWeather?> = stored

        override suspend fun saveWeatherData(
            weatherData: WeatherData,
            airQualityData: AirQualityData,
        ) {
            saveWeatherDataCallCount++
            stored.value = StoredWeather(weatherData, airQualityData)
        }

        override suspend fun clearAllData() {
            stored.value = null
        }
    }

    private class FakeWeatherWearSync : WeatherWearSync {
        var syncCallCount = 0
            private set
        var lastLocationName: String? = null
            private set

        override suspend fun sync(
            locationName: String,
            forecast: NetworkWeatherForecast,
        ) {
            syncCallCount++
            lastLocationName = locationName
        }
    }

    private class FakeHomeGeocoder(
        private val name: String? = "Fake Place",
        private val failure: Exception? = null,
    ) : HomeGeocoder {
        var calls = 0
            private set

        override suspend fun reverseGeocode(
            latitude: Double,
            longitude: Double,
        ): String? {
            calls++
            failure?.let { throw it }
            return name
        }
    }

    private companion object {
        const val LAT = 12.97
        const val LON = 77.59
        val COORDINATES = LAT to LON
        val GPS = ActiveCoordinates(Coordinates(LAT, LON))
        const val FIVE_MINUTES_MS = 5 * 60 * 1000L
        const val THIRTY_MINUTES_MS = 30 * 60 * 1000L
        const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
    }
}
