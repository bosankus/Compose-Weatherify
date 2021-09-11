package bose.ankush.weatherify.view

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import bose.ankush.weatherify.MainCoroutineRule
import bose.ankush.weatherify.data.model.AvgForecast
import bose.ankush.weatherify.getOrAwaitValue
import bose.ankush.weatherify.util.ResultData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel

    @get: Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        viewModel = MainViewModel(FakeWeatherRepository())
    }

    @Test
    fun fetchClimateDetails_ReturnsForecastAndCurrentTemperature() {
        viewModel.fetchClimateDetails()

        val currentTempLiveData = viewModel.currentTemp.getOrAwaitValue()
        val forecastLiveData = viewModel.forecast.getOrAwaitValue()

        assertThat(currentTempLiveData).isNotEqualTo(ResultData.Success(null))
        assertThat(forecastLiveData).isNotEqualTo(ResultData.Success(null))
    }
}