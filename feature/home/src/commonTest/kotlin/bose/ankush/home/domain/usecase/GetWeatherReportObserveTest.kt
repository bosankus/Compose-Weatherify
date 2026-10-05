package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.domain.repository.WeatherRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetWeatherReportObserveTest {
    @Test
    fun observeWithRefreshEmitsCacheBeforeRefreshFinishes() =
        runTest {
            val cached =
                WeatherForecast(
                    id = 7L,
                    lastUpdated = 1L,
                    current =
                        WeatherForecast.Current(
                            clouds = 0,
                            dt = 1L,
                            feels_like = 10.0,
                            humidity = 1,
                            pressure = 1000,
                            sunrise = 1L,
                            sunset = 2L,
                            temp = 11.0,
                            uvi = 0.0,
                            weather = emptyList(),
                            wind_gust = null,
                            wind_speed = 1.0,
                        ),
                )
            val release = CompletableDeferred<Unit>()
            var started = false
            var ended = false
            val repository =
                object : WeatherRepository {
                    private val weather = MutableStateFlow<WeatherForecast?>(cached)

                    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality> =
                        MutableStateFlow(AirQuality())

                    override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?> = weather

                    override suspend fun refreshWeatherData(
                        coordinates: Pair<Double, Double>,
                        forceRefresh: Boolean,
                    ) {
                        release.await()
                    }

                    override suspend fun clearAllData() = Unit
                }
            val useCase = GetWeatherReport(repository)
            val values = mutableListOf<WeatherForecast?>()
            val job =
                launch {
                    useCase
                        .observeWithRefresh(
                            location = 1.0 to 2.0,
                            refresh = { repository.refreshWeatherData(1.0 to 2.0, forceRefresh = false) },
                            onRefreshStart = { started = true },
                            onRefreshEnd = { ended = true },
                        ).collect { values.add(it) }
                }
            advanceUntilIdle()
            assertTrue(started)
            assertFalse(ended)
            assertEquals(11.0, values.first()?.current?.temp)
            release.complete(Unit)
            advanceUntilIdle()
            assertTrue(ended)
            job.cancel()
        }
}
