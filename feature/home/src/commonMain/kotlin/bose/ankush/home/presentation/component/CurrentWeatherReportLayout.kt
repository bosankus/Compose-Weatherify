package bose.ankush.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.degree
import bose.ankush.home.generated.resources.ic_humidity
import bose.ankush.home.generated.resources.ic_sunny
import bose.ankush.home.generated.resources.ic_uv
import bose.ankush.home.generated.resources.ic_wind
import bose.ankush.home.generated.resources.not_available
import bose.ankush.home.generated.resources.weather_icon_content
import bose.ankush.home.presentation.util.formatTextCapitalization
import bose.ankush.home.presentation.util.getFormattedDateTimeFromEpoch
import bose.ankush.home.presentation.util.getIconUrl
import bose.ankush.home.presentation.util.toCelsius
import coil3.compose.AsyncImage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Instant

@Composable
internal fun CurrentWeatherReportLayout(
    currentWeather: WeatherForecast.Current,
    userLocation: Pair<Double, Double>? = null,
    summary: String? = null,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LocationAndDateHeader(currentWeather, userLocation)

            Spacer(modifier = Modifier.height(24.dp))

            CurrentWeatherVisualization(currentWeather)

            Spacer(modifier = Modifier.height(32.dp))

            WeatherMetricsGrid(currentWeather)

            Spacer(modifier = Modifier.height(16.dp))

            SunriseSunsetInfo(currentWeather)

            summary?.let { summaryText ->
                if (summaryText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                            ),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Today's Forecast",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationAndDateHeader(
    currentWeather: WeatherForecast.Current,
    userLocation: Pair<Double, Double>? = null,
) {
    val geocoder = koinInject<HomeGeocoder>()
    var locationName by remember(userLocation) { mutableStateOf("Current Location") }

    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            locationName = geocoder.reverseGeocode(userLocation.first, userLocation.second) ?: "Current Location"
        }
    }

    val formattedDate =
        remember(currentWeather.dt) {
            getFormattedDateTimeFromEpoch(currentWeather.dt)
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = locationName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CurrentWeatherVisualization(currentWeather: WeatherForecast.Current) {
    val notAvailable = stringResource(Res.string.not_available)
    val firstWeather = currentWeather.weather?.firstOrNull()
    val weatherDescription = (firstWeather?.description ?: notAvailable).formatTextCapitalization()
    val weatherIconUrl = firstWeather?.icon?.getIconUrl()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(Res.string.degree, currentWeather.temp?.toCelsius() ?: notAvailable),
                style = MaterialTheme.typography.displayLarge,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Feels like ${currentWeather.feels_like?.toCelsius()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(100.dp),
            ) {
                AsyncImage(
                    model = weatherIconUrl,
                    placeholder = painterResource(Res.drawable.ic_sunny),
                    contentDescription = stringResource(Res.string.weather_icon_content),
                    modifier =
                        Modifier
                            .padding(16.dp)
                            .size(64.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = weatherDescription,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeatherMetricsGrid(weatherData: WeatherForecast.Current) {
    val firstRowMetrics =
        listOf(
            WeatherMetric(icon = Res.drawable.ic_humidity, value = "${weatherData.humidity}%", label = "Humidity"),
            WeatherMetric(icon = Res.drawable.ic_wind, value = "${weatherData.wind_speed} m/s", label = "Wind"),
            WeatherMetric(icon = Res.drawable.ic_uv, value = "${weatherData.uvi}", label = "UV Index"),
        )

    val secondRowMetrics =
        mutableListOf(
            WeatherMetric(icon = Res.drawable.ic_humidity, value = "${weatherData.pressure} hPa", label = "Pressure"),
            WeatherMetric(icon = Res.drawable.ic_humidity, value = "${weatherData.clouds}%", label = "Clouds"),
        )

    if (weatherData.wind_gust != null) {
        secondRowMetrics.add(
            WeatherMetric(icon = Res.drawable.ic_wind, value = "${weatherData.wind_gust} m/s", label = "Wind Gust"),
        )
    }

    MetricsRow(metrics = firstRowMetrics)

    Spacer(modifier = Modifier.height(16.dp))

    MetricsRow(metrics = secondRowMetrics, fillEmptySpace = true)
}

@Composable
private fun WeatherMetricItem(
    icon: DrawableResource,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 4.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(8.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun SunriseSunsetInfo(weatherData: WeatherForecast.Current) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Sunrise",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )

                Text(
                    text = formatTimeWithAmPm(weatherData.sunrise, true),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Box(
                modifier =
                    Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Sunset",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )

                Text(
                    text = formatTimeWithAmPm(weatherData.sunset, false),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun formatTimeWithAmPm(
    timestamp: Long?,
    isSunrise: Boolean,
): String {
    if (timestamp == null) return "N/A"

    return remember(timestamp, isSunrise) {
        val localDateTime = Instant.fromEpochSeconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        val hour12 =
            when {
                localDateTime.hour == 0 -> 12
                localDateTime.hour > 12 -> localDateTime.hour - 12
                else -> localDateTime.hour
            }
        val minute = localDateTime.minute.toString().padStart(2, '0')
        val timeWithoutAmPm = "$hour12:$minute"
        if (isSunrise) "$timeWithoutAmPm AM" else "$timeWithoutAmPm PM"
    }
}

private data class WeatherMetric(
    val icon: DrawableResource,
    val value: String,
    val label: String,
)

@Composable
private fun MetricsRow(
    metrics: List<WeatherMetric>,
    fillEmptySpace: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        metrics.forEach { metric ->
            WeatherMetricItem(
                icon = metric.icon,
                value = metric.value,
                label = metric.label,
                modifier = Modifier.weight(1f),
            )
        }

        if (fillEmptySpace && metrics.size < 3) {
            repeat(3 - metrics.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
