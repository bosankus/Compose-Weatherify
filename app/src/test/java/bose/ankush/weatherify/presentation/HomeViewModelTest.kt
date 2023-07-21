package bose.ankush.weatherify.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import bose.ankush.weatherify.MainCoroutineRule
import bose.ankush.weatherify.base.common.ResultData
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.data.preference.PreferenceManager
import bose.ankush.weatherify.domain.repository.WeatherRepository
import bose.ankush.weatherify.domain.use_case.get_air_quality.GetAirQuality
import bose.ankush.weatherify.domain.use_case.get_weather_forecasts.GetForecasts
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetTodaysWeatherReport
import bose.ankush.weatherify.presentation.home.HomeViewModel
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString

class HomeViewModelTest {

    @get: Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    val mainCoroutineRule = MainCoroutineRule()

    @RelaxedMockK
    private lateinit var getTodayWeatherUseCase: GetTodaysWeatherReport

    @RelaxedMockK
    private lateinit var getForecastsUseCase: GetForecasts

    @RelaxedMockK
    private lateinit var preferenceManager: PreferenceManager

    @RelaxedMockK
    private lateinit var getAirQuality: GetAirQuality

    @RelaxedMockK
    private lateinit var locationClient: LocationClient

    private lateinit var viewModel: HomeViewModel

    /**
     * this method is helps to initiate mockk and setup mocked objects before tests are run
     */
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(WeatherRepository::class)
        viewModel = HomeViewModel(
            getTodayWeatherUseCase,
            getForecastsUseCase,
            getAirQuality,
            locationClient,
            preferenceManager
        )
    }

    /**
     * this method runs at the end of tests to unmockk all mocked objects
     */
    @After
    fun teardown() {
        unmockkAll()
    }

    /**
     * testing main method to fetch weather details.
     * this tests that the use cases are working as expected and returning series of flows
     * contains loading, success and failure states
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `fetchWeatherDetails, calls uses cases and fetch flow data successfully`() =
        mainCoroutineRule.testScope.runTest {
            coEvery { getTodayWeatherUseCase(anyString()) } returns
                    flowOf(ResultData.Loading, ResultData.Success(null), ResultData.Failed(null))
            coEvery { getForecastsUseCase(anyString()) } returns
                    flowOf(ResultData.Loading, ResultData.Success(null), ResultData.Failed(null))

            viewModel.fetchWeatherDetails(anyString())

            getTodayWeatherUseCase.invoke(anyString()).test {
                assertEquals(ResultData.Loading, awaitItem())
                assertEquals(ResultData.Success(null), awaitItem())
                assertEquals(ResultData.Failed(null), awaitItem())
                awaitComplete()
            }

            getForecastsUseCase.invoke(anyString()).test {
                assertEquals(ResultData.Loading, awaitItem())
                assertEquals(ResultData.Success(null), awaitItem())
                assertEquals(ResultData.Failed(null), awaitItem())
                awaitComplete()
            }
        }
}