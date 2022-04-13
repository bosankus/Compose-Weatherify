package bose.ankush.weatherify.view

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import bose.ankush.weatherify.MainCoroutineRule
import bose.ankush.weatherify.data.WeatherRepository
import bose.ankush.weatherify.data.model.CurrentTemperature
import bose.ankush.weatherify.util.ResultData
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
    fun getCurrentTemperature_returns_correctFlowResultData() {
        runTest {
            // List to collect the results
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val result = arrayListOf<ResultData<CurrentTemperature>>()
            val job = launch(dispatcher) { viewModel.temperature.toList(result) }

            // Act
            viewModel.getCurrentTemperature()
            runCurrent()

            // Assert
            assertThat(result).isEqualTo(
                listOf(
                    ResultData.DoNothing,
                    ResultData.Loading,
                    ResultData.Success(CurrentTemperature.Main(307.12))
                )
            )

            // Cancelling job
            job.cancel()
        }
    }
}