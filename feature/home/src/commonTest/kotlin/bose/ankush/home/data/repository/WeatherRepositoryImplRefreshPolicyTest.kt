package bose.ankush.home.data.repository

import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.repository.WeatherWearSync
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.model.AirQualityData
import bose.ankush.storage.model.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import bose.ankush.network.model.WeatherForecast as NetworkWeatherForecast
import bose.ankush.network.repository.WeatherRepository as NetworkWeatherRepository

/**
 * Exercises the cache/network refresh gate inside [WeatherRepositoryImpl]:
 * network is hit only when [forceRefresh] is true or the last update is older than one hour.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplRefreshPolicyTest {
    @Test
    fun refreshWeatherData_skipsNetworkWhenCacheIsFresh() =
        runTest {
            val now = Clock.System.now().toEpochMilliseconds()
            val network = FakeNetworkWeatherRepository()
            val storage =
                FakeWeatherStorage(lastUpdateTime = now - THIRTY_MINUTES_MS)
            val repo = createRepository(network, storage)

            repo.refreshWeatherData(COORDINATES, forceRefresh = false)

            assertEquals(0, network.refreshCallCount)
            assertEquals(0, storage.saveWeatherDataCallCount)
            assertEquals(0, storage.saveLastUpdateCallCount)
        }

    @Test
    fun refreshWeatherData_hitsNetworkWhenCacheIsStale() =
        runTest {
            val now = Clock.System.now().toEpochMilliseconds()
            val network = FakeNetworkWeatherRepository()
            val storage =
                FakeWeatherStorage(lastUpdateTime = now - TWO_HOURS_MS)
            val wearSync = FakeWeatherWearSync()
            val geocoder = FakeHomeGeocoder(name = "Test City")
            val repo = createRepository(network, storage, wearSync, geocoder)

            repo.refreshWeatherData(COORDINATES, forceRefresh = false)

            assertEquals(1, network.refreshCallCount)
            assertEquals(1, storage.saveWeatherDataCallCount)
            assertEquals(1, storage.saveLastUpdateCallCount)
            assertEquals(1, wearSync.syncCallCount)
            assertEquals("Test City", wearSync.lastLocationName)
            assertTrue(storage.lastSavedUpdateTime!! >= now)
        }

    @Test
    fun refreshWeatherData_forceRefreshHitsNetworkEvenWhenFresh() =
        runTest {
            val now = Clock.System.now().toEpochMilliseconds()
            val network = FakeNetworkWeatherRepository()
            val storage =
                FakeWeatherStorage(lastUpdateTime = now - FIVE_MINUTES_MS)
            val repo = createRepository(network, storage)

            repo.refreshWeatherData(COORDINATES, forceRefresh = true)

            assertEquals(1, network.refreshCallCount)
            assertEquals(1, storage.saveWeatherDataCallCount)
        }

    @Test
    fun refreshWeatherData_treatsMissingLastUpdateAsStale() =
        runTest {
            val network = FakeNetworkWeatherRepository()
            val storage = FakeWeatherStorage(lastUpdateTime = 0L)
            val repo = createRepository(network, storage)

            repo.refreshWeatherData(COORDINATES, forceRefresh = false)

            assertEquals(1, network.refreshCallCount)
            assertEquals(1, storage.saveWeatherDataCallCount)
        }

    @Test
    fun refreshWeatherData_networkFailureKeepsCacheWithoutSaving() =
        runTest {
            val now = Clock.System.now().toEpochMilliseconds()
            val network =
                FakeNetworkWeatherRepository(
                    result = Result.failure(IllegalStateException("offline")),
                )
            val storage =
                FakeWeatherStorage(lastUpdateTime = now - TWO_HOURS_MS)
            val wearSync = FakeWeatherWearSync()
            val repo = createRepository(network, storage, wearSync)

            repo.refreshWeatherData(COORDINATES, forceRefresh = false)

            assertEquals(1, network.refreshCallCount)
            assertEquals(0, storage.saveWeatherDataCallCount)
            assertEquals(0, storage.saveLastUpdateCallCount)
            assertEquals(0, wearSync.syncCallCount)
            assertEquals(now - TWO_HOURS_MS, storage.lastUpdateTime)
        }

    @Test
    fun refreshWeatherData_usesDefaultLocationNameWhenGeocodeNull() =
        runTest {
            val now = Clock.System.now().toEpochMilliseconds()
            val network = FakeNetworkWeatherRepository()
            val storage =
                FakeWeatherStorage(lastUpdateTime = now - TWO_HOURS_MS)
            val wearSync = FakeWeatherWearSync()
            val geocoder = FakeHomeGeocoder(name = null)
            val repo = createRepository(network, storage, wearSync, geocoder)

            repo.refreshWeatherData(COORDINATES, forceRefresh = true)

            assertEquals("Current Location", wearSync.lastLocationName)
        }

    private fun createRepository(
        network: FakeNetworkWeatherRepository,
        storage: FakeWeatherStorage,
        wearSync: FakeWeatherWearSync = FakeWeatherWearSync(),
        geocoder: FakeHomeGeocoder = FakeHomeGeocoder(),
    ) = WeatherRepositoryImpl(
        networkRepository = network,
        weatherStorage = storage,
        weatherWearSync = wearSync,
        homeGeocoder = geocoder,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private class FakeNetworkWeatherRepository(
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
    ) : NetworkWeatherRepository {
        var refreshCallCount = 0
            private set

        override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>): Result<NetworkWeatherForecast> {
            refreshCallCount++
            assertEquals(COORDINATES, coordinates)
            return result
        }
    }

    private class FakeWeatherStorage(
        var lastUpdateTime: Long,
    ) : WeatherStorage {
        private val weather = MutableStateFlow<WeatherData?>(null)
        private val airQuality = MutableStateFlow<AirQualityData?>(null)

        var saveWeatherDataCallCount = 0
            private set
        var saveLastUpdateCallCount = 0
            private set
        var lastSavedUpdateTime: Long? = null
            private set

        override fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<WeatherData?> = weather

        override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQualityData?> = airQuality

        override suspend fun getLastWeatherUpdateTime(coordinates: Pair<Double, Double>): Long = lastUpdateTime

        override suspend fun saveLastWeatherUpdateTime(
            coordinates: Pair<Double, Double>,
            time: Long,
        ) {
            saveLastUpdateCallCount++
            lastUpdateTime = time
            lastSavedUpdateTime = time
        }

        override suspend fun saveWeatherData(
            weatherData: WeatherData,
            airQualityData: AirQualityData,
        ) {
            saveWeatherDataCallCount++
            weather.value = weatherData
            airQuality.value = airQualityData
        }

        override suspend fun clearAllData() {
            weather.value = null
            airQuality.value = null
            lastUpdateTime = 0L
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
    ) : HomeGeocoder {
        override suspend fun reverseGeocode(
            latitude: Double,
            longitude: Double,
        ): String? = name
    }

    private companion object {
        val COORDINATES = 12.97 to 77.59
        const val FIVE_MINUTES_MS = 5 * 60 * 1000L
        const val THIRTY_MINUTES_MS = 30 * 60 * 1000L
        const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
    }
}
