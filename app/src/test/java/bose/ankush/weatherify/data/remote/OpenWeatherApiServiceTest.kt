package bose.ankush.weatherify.data.remote

import bose.ankush.weatherify.MainCoroutineRule
import bose.ankush.weatherify.MockWebServerUtil.enqueueResponse
import bose.ankush.weatherify.data.remote.api.OpenWeatherApiService
import bose.ankush.weatherify.domain.model.AirQuality
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OpenWeatherApiServiceTest {

    private val mockWebServer = MockWebServer()

    private lateinit var client: OkHttpClient
    private lateinit var openWeatherApiService: OpenWeatherApiService

    @get: Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        mockWebServer.start()

        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()

        openWeatherApiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenWeatherApiService::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getCurrentAirQuality should fetch response successfully with 200 code`() =
        mainCoroutineRule.testScope.runTest {
            try {
                mockWebServer.enqueueResponse("air_quality.json", 200)
                val actualResponse = openWeatherApiService.getCurrentAirQuality(
                    latitude = "22.48",
                    longitude = "88.40"
                )
                val expectedResponse = AirQuality(
                    aqi = 5,
                    co = 1441.96,
                    no2 = 40.44,
                    o3 = 34.69,
                    so2 = 16.45,
                    pm10 = 237.01,
                    pm25 = 165.02
                )
                Truth.assertThat(actualResponse).isEqualTo(expectedResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
}
