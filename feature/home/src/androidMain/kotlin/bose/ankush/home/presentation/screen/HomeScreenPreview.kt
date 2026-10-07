package bose.ankush.home.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherCondition
import bose.ankush.home.domain.model.WeatherForecast
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

data class HomePreviewMock(
    val temperature: String,
    val place: String,
    val condition: SkyCondition,
    val feel: String,
    val wind: String,
    val uv: String,
)

object HomePreviewData {
    val home =
        HomePreviewMock(
            temperature = "16°",
            place = "London",
            condition = SkyCondition.FOG,
            feel = "16°",
            wind = "WSW 6 mph",
            uv = "7",
        )
}

private val previewDays =
    listOf(
        CalendarStripDay("Sun", "16", "16°", selected = false),
        CalendarStripDay("Mon", "17", "15°", selected = false),
        CalendarStripDay("Tue", "18", "14°", selected = false),
        CalendarStripDay("Wed", "19", "16°", selected = true),
        CalendarStripDay("Thu", "20", "17°", selected = false),
        CalendarStripDay("Fri", "21", "18°", selected = false),
        CalendarStripDay("Sat", "22", "16°", selected = false),
    )

@Preview(showBackground = true, widthDp = 390, heightDp = 844, backgroundColor = 0xFF101418)
@Composable
internal fun HomeScreenPreview() {
    val mock = HomePreviewData.home
    HomeScreen(
        modifier = Modifier,
        links = HomeScreenLinks(weather = {}, places = {}, onOpenHub = {}),
        shell =
            HomeScreenShell(
                content =
                    HomeWeatherContent(
                        temperature = mock.temperature,
                        place = mock.place,
                        condition = mock.condition,
                        feel = mock.feel,
                        wind = mock.wind,
                        uv = mock.uv,
                        days = previewDays,
                        showSmallCards = true,
                    ),
                photo = null,
            ),
        sections = previewSections,
    )
}

private val previewNow = Clock.System.now().epochSeconds

private val previewSections =
    HomeScreenSections(
        nearby =
            NearbyContent(
                showAccount = true,
                events =
                    listOf(
                        NearbyEventLine(title = "Farmers market", whenLabel = "Today, 10:00"),
                        NearbyEventLine(title = "Open-air cinema", whenLabel = "Sat, 20:30"),
                    ),
                savedPlace =
                    FeaturedSavedPlace(
                        name = "Home",
                        subtitle = "London, England, GB",
                        message = "Light fog until noon",
                    ),
            ),
        forecast =
            ForecastDetails(
                alerts =
                    listOf(
                        WeatherForecast.Alert(
                            description = "Dense fog may reduce visibility below 200 m.",
                            end = previewNow + 6 * 3600,
                            event = "Fog warning",
                            sender_name = "Met Office",
                            start = previewNow,
                        ),
                    ),
                airQuality =
                    AirQuality(
                        aqi = 2,
                        co = 230.0,
                        no2 = 12.4,
                        o3 = 61.0,
                        so2 = 1.8,
                        pm10 = 14.2,
                        pm25 = 8.6,
                    ),
                hourly =
                    List(12) { i ->
                        WeatherForecast.Hourly(
                            clouds = 80,
                            dt = previewNow + i * 3600L,
                            feels_like = 288.6 + i * 0.2,
                            humidity = 82,
                            temp = 289.1 + i * 0.3, // Kelvin, like the API
                            weather =
                                listOf(
                                    WeatherCondition(
                                        description = "mist",
                                        icon = "50d",
                                        id = 701,
                                        main = "Mist",
                                    ),
                                ),
                        )
                    },
                extras =
                    ForecastExtras(
                        rainToday = "1.2 mm",
                        nextRain = "Rain around 15:00",
                        temperatureTrend =
                            TemperatureTrendData(
                                line = "Warming 3° by evening",
                                direction = TemperatureTrendDirection.WARMING,
                            ),
                    ),
            ),
        chrome =
            HomeScreenChrome(
                todaySummary = "Foggy morning, clearing by afternoon",
                eventDates = setOf(LocalDate(2026, 10, 9), LocalDate(2026, 10, 11)),
            ),
        places = SavedPlacesBinding(),
    )
