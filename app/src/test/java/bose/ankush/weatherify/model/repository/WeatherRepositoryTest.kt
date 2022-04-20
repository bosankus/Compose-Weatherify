package bose.ankush.weatherify.model.repository

import app.cash.turbine.test
import bose.ankush.weatherify.MainCoroutineRule
import bose.ankush.weatherify.model.model.CurrentTemperature
import bose.ankush.weatherify.model.network.ApiService
import bose.ankush.weatherify.util.ResultData
import bose.ankush.weatherify.util.TestDispatcher
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.buffer
import okio.source
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WeatherRepositoryTest {

    @get: Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val mockWebServer = MockWebServer()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private lateinit var api: ApiService
    private lateinit var repository: WeatherRepository
    private lateinit var testDispatcher: TestDispatcher


    @Before
    fun setUp() {
        mockWebServer.start()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        testDispatcher = TestDispatcher()
        repository = WeatherRepositoryImpl(api, testDispatcher)
    }


    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }


    @Test
    fun sampleTest() = runTest {

        mockWebServer.enqueueResponse("temperature.json", 200)

        val job = launch { repository.getWeatherForecast().test {
            val first = awaitItem()
            assertThat(first).isEqualTo(ResultData.Loading)
            val second = awaitError()
            assertThat(second).isEqualTo(ResultData.Success(
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
}

internal fun MockWebServer.enqueueResponse(fileName: String, code: Int) {
    val inputStream = javaClass.classLoader?.getResourceAsStream(fileName)

    val source = inputStream?.let { inputStream.source().buffer() }
    source?.let {
        enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(source.readString(StandardCharsets.UTF_8))
        )
    }
}
