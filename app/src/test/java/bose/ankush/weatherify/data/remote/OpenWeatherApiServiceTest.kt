package bose.ankush.weatherify.data.remote

import bose.ankush.weatherify.MockWebServerUtil.enqueueResponse
import bose.ankush.weatherify.data.remote.api.OpenWeatherApiService
import bose.ankush.weatherify.data.remote.dto.ForecastTestResponse
import bose.ankush.weatherify.data.remote.dto.toForecastTestResponse
import bose.ankush.weatherify.data.remote.dto.toWeather
import bose.ankush.weatherify.domain.model.Weather
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OpenWeatherApiServiceTest {

    private val mockWebServer = MockWebServer()

    private lateinit var client: OkHttpClient
    private lateinit var openWeatherApiService: OpenWeatherApiService


    @Before
    fun setup() {
        mockWebServer.start()

        client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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
        runBlocking {
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
        runBlocking {
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
}

