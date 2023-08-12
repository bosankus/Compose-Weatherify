package bose.ankush.weatherify.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import bose.ankush.weatherify.MainCoroutineRule
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.data.preference.PreferenceManager
import bose.ankush.weatherify.domain.repository.WeatherRepository
import bose.ankush.weatherify.domain.use_case.get_air_quality.GetAirQuality
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetWeatherReport
import bose.ankush.weatherify.domain.use_case.refresh_weather_reports.RefreshWeatherReport
import bose.ankush.weatherify.presentation.home.WeatherViewModel
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule

class WeatherViewModelTest {

    @get: Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    val mainCoroutineRule = MainCoroutineRule()

    @RelaxedMockK
    private lateinit var getWeatherReport: GetWeatherReport

    @RelaxedMockK
    private lateinit var refreshWeatherReport: RefreshWeatherReport

    @RelaxedMockK
    private lateinit var preferenceManager: PreferenceManager

    @RelaxedMockK
    private lateinit var getAirQuality: GetAirQuality

    @RelaxedMockK
    private lateinit var locationClient: LocationClient

    private lateinit var viewModel: WeatherViewModel

    /**
     * this method is helps to initiate mockk and setup mocked objects before tests are run
     */
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(WeatherRepository::class)
        viewModel = WeatherViewModel(
            refreshWeatherReport,
            getWeatherReport,
            getAirQuality,
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
    /*@Test
    fun `fetchWeatherDetails, calls uses cases and fetch flow data successfully`() =
        mainCoroutineRule.testScope.runTest {
            coEvery { getWeatherReport() } returns
                    flowOf(ResultData.Loading, ResultData.Success(null), ResultData.Failed(null))

            viewModel.performInitialDataLoading()

            // TODO: Add coverage
        }*/
}