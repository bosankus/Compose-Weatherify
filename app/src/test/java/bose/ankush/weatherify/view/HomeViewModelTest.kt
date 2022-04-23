package bose.ankush.weatherify.view

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import bose.ankush.weatherify.MainCoroutineRule
import bose.ankush.weatherify.model.repository.WeatherRepository
import bose.ankush.weatherify.model.model.CurrentTemperature
import bose.ankush.weatherify.util.ResultData
import bose.ankush.weatherify.view.home.HomeViewModel
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class HomeViewModelTest {

    @get: Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val dataSource: WeatherRepository = mock()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = HomeViewModel(dataSource)
    }


    @Test
    fun `getTemperature, returns data successfully via flow`() = runTest {
        viewModel.fetchClimateDetails()
        val job = launch { viewModel.temperature.test {
            val first = awaitItem()
            assertThat(first).isEqualTo(ResultData.DoNothing)
            val second = awaitItem()
            assertThat(second).isEqualTo(ResultData.Loading)
            val third = awaitItem()
            assertThat(third).isEqualTo(ResultData.Success(
                CurrentTemperature(
                    cod = 200,
                    main = CurrentTemperature.Main(temp = 305.12),
                    name = "Kolkata",
                    weather = listOf(CurrentTemperature.Weather(icon = "50d"))
                )
            ))
        } }

        job.join()
        job.cancel()
    }

    @Test
    fun `getCurrentTemperature, returns data successfully via flow`() = runTest {
        viewModel.fetchClimateDetails()
        val job = launch { viewModel.forecast.test {
            val first = awaitItem()
            assertThat(first).isEqualTo(ResultData.DoNothing)
            val second = awaitItem()
            assertThat(second).isEqualTo(ResultData.Loading)
            val third = awaitItem()
            assertThat(third).isEqualTo(ResultData.Success(null))
        } }

        job.join()
        job.cancel()
    }
}