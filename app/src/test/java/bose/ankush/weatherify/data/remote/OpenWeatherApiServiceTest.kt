package bose.ankush.weatherify.data.remote

import bose.ankush.weatherify.MainCoroutineRule
import bose.ankush.weatherify.MockWebServerUtil.enqueueResponse
import bose.ankush.weatherify.data.remote.dto.ForecastTestResponse
import bose.ankush.weatherify.data.remote.dto.toAirQuality
import bose.ankush.weatherify.data.remote.dto.toForecastTestResponse
import bose.ankush.weatherify.data.remote.dto.toWeather
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.Weather
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun `getTodaysWeatherReport should fetch response successfully with 200 code`(): Unit =
        mainCoroutineRule.testScope.runTest {
            try {
                mockWebServer.enqueueResponse("weather_report.json", 200)
                val actualResponse =
                    openWeatherApiService.getTodaysWeatherReport("Kolkata").toWeather()
                val expectedResponse = Weather(
                    cod = 200,
                    temp = 294.16,
                    humidity = 39,
                    wind = 2.36,
                    windAngle = 54,
                    name = "Kolkata",
                    icon = "01n",
                )
                assertThat(actualResponse).isEqualTo(expectedResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    @Test
    fun `getWeatherForecastList should fetch response successfully with 200 code`(): Unit =
        mainCoroutineRule.testScope.runTest {
            try {
                mockWebServer.enqueueResponse("weather_forecast.json", 200)
                val actualResponse = openWeatherApiService.getWeatherForecastList("Kolkata")
                    .toForecastTestResponse()
                val expectedResponse = ForecastTestResponse(
                    cod = "200",
                    cnt = 1,
                    windSpeed = 2.46,
                    cityName = "Kolkata"
                )
                assertThat(actualResponse).isEqualTo(expectedResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    @Test
    fun `getCurrentAirQuality should fetch response successfully with 200 code`() =
        mainCoroutineRule.testScope.runTest {
            try {
                mockWebServer.enqueueResponse("air_quality.json", 200)
                val actualResponse = openWeatherApiService.getCurrentAirQuality(
                    latitude = "22.48",
                    longitude = "88.40"
                ).toAirQuality()
                val expectedResponse = AirQuality(
                    aqi = 5,
                    co = 1441.96,
                    no2 = 40.44,
                    o3 = 34.69,
                    so2 = 16.45,
                    pm10 = 237.01,
                    pm25 = 165.02
                )
                assertThat(actualResponse).isEqualTo(expectedResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
}
