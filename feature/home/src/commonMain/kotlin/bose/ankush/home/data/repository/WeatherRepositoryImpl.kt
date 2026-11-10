package bose.ankush.home.data.repository

import bose.ankush.home.data.mapper.AirQualityMapper
import bose.ankush.home.data.mapper.NetworkToStorageMapper
import bose.ankush.home.data.mapper.WeatherMapper
import bose.ankush.home.domain.location.ActiveCoordinates
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.location.isServedBy
import bose.ankush.home.domain.location.point
import bose.ankush.home.domain.model.RefreshOutcome
import bose.ankush.home.domain.model.WeatherSnapshot
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.home.domain.repository.WeatherWearSync
import bose.ankush.network.repository.WeatherRemoteSource
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.model.WeatherData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

internal class WeatherRepositoryImpl(
    private val weatherRemoteSource: WeatherRemoteSource,
    private val weatherStorage: WeatherStorage,
    private val weatherWearSync: WeatherWearSync,
    private val homeGeocoder: HomeGeocoder,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WeatherRepository {
    override val snapshot: Flow<WeatherSnapshot?> =
        weatherStorage.observeWeather().map { stored ->
            stored?.let {
                WeatherMapper.mapToDomain(it.weather)?.let { forecast ->
                    WeatherSnapshot(
                        forecast = forecast,
                        airQuality = it.airQuality?.let(AirQualityMapper::mapToDomain),
                        point = it.weather.point(),
                        placeName = it.weather.placeName,
                    )
                }
            }
        }

    /**
     * Fetch /weather, label it with [place]'s point and name, and save weather and air quality
     * in one write. Air quality is embedded in the response and may be null for free-tier users;
     * an empty AirQualityData is stored then to satisfy the storage contract.
     *
     * The saved row is fresh only for its own place: a different spot, or a row from before
     * schema 6 with no point, is refetched however recent it is.
     */
    override suspend fun refresh(
        place: ActiveCoordinates,
        forceRefresh: Boolean,
    ): Result<RefreshOutcome> =
        withContext(ioDispatcher) {
            val saved = weatherStorage.observeWeather().first()?.weather
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val isFresh =
                !forceRefresh &&
                        saved != null &&
                        place.isServedBy(saved.point()) &&
                        currentTime - saved.lastUpdated <= ONE_HOUR_IN_MILLIS
            if (isFresh) return@withContext Result.success(RefreshOutcome.UpToDate)

            val point = place.point
            coroutineScope {
                // The name is looked up alongside the request, so the row is written once, named.
                val name =
                    async { place.overrideName?.takeIf { it.isNotBlank() } ?: lookUpName(point) }
                // A failure leaves the saved snapshot alone; the caller decides how to show it.
                weatherRemoteSource.refreshWeatherData(point.lat to point.lon).map {
                    val placeName = name.await()
                    val weatherStorageData =
                        NetworkToStorageMapper
                            .mapWeatherToStorageEntity(it)
                            .copy(
                                latitude = point.lat,
                                longitude = point.lon,
                                placeName = placeName
                            )
                    val airQualityStorageData =
                        NetworkToStorageMapper.mapAirQualityToStorageEntity(it.data?.airQuality)
                    weatherStorage.saveWeatherData(weatherStorageData, airQualityStorageData)
                    weatherWearSync.sync(placeName ?: DEFAULT_LOCATION_NAME, it)
                    RefreshOutcome.Updated
                }.also { name.cancel() }
            }
        }

    /** A failed lookup leaves the place unnamed; it must not fail the refresh. */
    private suspend fun lookUpName(point: GeoPoint): String? =
        try {
            homeGeocoder.reverseGeocode(point.lat, point.lon)?.takeIf { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    override suspend fun clearAllData() {
        withContext(ioDispatcher) {
            weatherStorage.clearAllData()
        }
    }

    companion object {
        private const val ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L
        private const val DEFAULT_LOCATION_NAME = "Current Location"
    }
}

private fun WeatherData.point(): GeoPoint? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return GeoPoint(lat, lon)
}
