package bose.ankush.weatherify.view

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import bose.ankush.weatherify.MainCoroutineRule
import bose.ankush.weatherify.model.repository.WeatherRepository
import bose.ankush.weatherify.model.model.CurrentTemperature
import bose.ankush.weatherify.util.ResultData
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MainViewModelTest {

    @get: Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val dataSource: WeatherRepository = mock()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = MainViewModel(dataSource)
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