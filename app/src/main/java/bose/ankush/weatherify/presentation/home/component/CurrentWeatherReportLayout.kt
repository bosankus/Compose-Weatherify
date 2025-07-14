package bose.ankush.weatherify.presentation.home.component

import android.location.Geocoder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.sunriseui.SunriseSunsetCombinedAnimation
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.DateTimeUtils
import bose.ankush.weatherify.base.common.Extension.formatTextCapitalization
import bose.ankush.weatherify.base.common.Extension.getIconUrl
import bose.ankush.weatherify.base.common.Extension.toCelsius
import bose.ankush.weatherify.domain.model.WeatherForecast
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CurrentWeatherReportLayout(
    currentWeather: WeatherForecast.Current,
    userLocation: Pair<Double, Double>? = null,
    summary: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Location and date
            LocationAndDateHeader(currentWeather, userLocation)

            Spacer(modifier = Modifier.height(24.dp))

            // Current weather visualization
            CurrentWeatherVisualization(currentWeather)

            Spacer(modifier = Modifier.height(32.dp))

            // Weather metrics
            WeatherMetricsGrid(currentWeather)

            Spacer(modifier = Modifier.height(16.dp))

            // Sunrise and sunset info
            SunriseSunsetInfo(currentWeather)

            // Weather summary
            summary?.let { summaryText ->
                if (summaryText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Today's Forecast",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
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
    userLocation: Pair<Double, Double>? = null
) {
    val context = LocalContext.current
    // Use remember to avoid recreating the state on each recomposition
    var locationName by remember(userLocation) { mutableStateOf("Current Location") }

    // Move Geocoder operation to LaunchedEffect but with IO dispatcher to avoid blocking UI
    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            try {
                // Use IO dispatcher for background processing
                val result = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(context, Locale.getDefault())

                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(
                        userLocation.first,
                        userLocation.second,
                        1
                    )

                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val cityName = address.locality ?: address.subAdminArea
                        val countryName = address.countryName

                        when {
                            cityName != null -> "$cityName, $countryName"
                            else -> countryName ?: "Current Location"
                        }
                    } else {
                        "Current Location"
                    }
                }
                // Update state only once after background processing is complete
                locationName = result
            } catch (e: Exception) {
                // If geocoding fails, keep the default "Current Location"
                e.printStackTrace()
            }
        }
    }

    // Pre-calculate the formatted date to avoid doing it during composition
    val formattedDate = remember(currentWeather.dt) {
        DateTimeUtils.getFormattedDateTimeFromEpoch(currentWeather.dt)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display the location name
        Text(
            text = locationName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Temperature display
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(
                    id = R.string.degree,
                    currentWeather.temp?.toCelsius() ?: stringResource(id = R.string.not_available)
                ),
                style = MaterialTheme.typography.displayLarge,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Feels like ${currentWeather.feels_like?.toCelsius()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Weather icon and description
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(100.dp)
            ) {
                AsyncImage(
                    model = currentWeather.weather?.get(0)?.icon?.getIconUrl(),
                    placeholder = painterResource(id = R.drawable.ic_sunny),
                    contentDescription = stringResource(id = R.string.weather_icon_content),
                    modifier = Modifier
                        .padding(16.dp)
                        .size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = (currentWeather.weather?.get(0)?.description
                    ?: stringResource(id = R.string.not_available)).formatTextCapitalization(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WeatherMetricsGrid(weatherData: WeatherForecast.Current) {
    // First row metrics
    val firstRowMetrics = listOf(
        WeatherMetric(
            icon = R.drawable.ic_humidity,
            value = "${weatherData.humidity}%",
            label = "Humidity"
        ),
        WeatherMetric(
            icon = R.drawable.ic_wind,
            value = "${weatherData.wind_speed} m/s",
            label = "Wind"
        ),
        WeatherMetric(
            icon = R.drawable.ic_uv,
            value = "${weatherData.uvi}",
            label = "UV Index"
        )
    )

    // Second row metrics
    val secondRowMetrics = mutableListOf(
        WeatherMetric(
            icon = R.drawable.ic_humidity, // Using humidity icon for pressure as it's more appropriate than sunny
            value = "${weatherData.pressure} hPa",
            label = "Pressure"
        ),
        WeatherMetric(
            icon = R.drawable.ic_humidity, // Using humidity icon for clouds as it's more appropriate than sunny
            value = "${weatherData.clouds}%",
            label = "Clouds"
        )
    )

    // Add wind gust if available
    if (weatherData.wind_gust != null) {
        secondRowMetrics.add(
            WeatherMetric(
                icon = R.drawable.ic_wind,
                value = "${weatherData.wind_gust} m/s",
                label = "Wind Gust"
            )
        )
    }

    // Display the metrics rows
    MetricsRow(metrics = firstRowMetrics)

    Spacer(modifier = Modifier.height(16.dp))

    MetricsRow(metrics = secondRowMetrics, fillEmptySpace = true)
}

@Composable
private fun WeatherMetricItem(
    icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SunriseSunsetInfo(weatherData: WeatherForecast.Current) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            SunriseSunsetCombinedAnimation(
                sunriseTimestamp = weatherData.sunrise?.toLong(),
                sunsetTimestamp = weatherData.sunset?.toLong(),
                currentTimestamp = System.currentTimeMillis() / 1000
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sunrise
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Sunrise",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Text(
                        text = formatTimeWithAmPm(
                            weatherData.sunrise,
                            true
                        ), // Force AM for sunrise
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )

                // Sunset
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Sunset",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Text(
                        text = formatTimeWithAmPm(weatherData.sunset, false), // Force PM for sunset
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


// Create SimpleDateFormat instances to be reused
private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
private val hourMinuteFormatter = SimpleDateFormat("h:mm", Locale.getDefault())

@Composable
private fun formatTime(timestamp: Int?): String {
    if (timestamp == null) return "N/A"

    // Use remember to cache the formatted time based on the timestamp
    return remember(timestamp) {
        val date = Date(timestamp.toLong() * 1000)
        timeFormatter.format(date)
    }
}

@Composable
private fun formatTimeWithAmPm(timestamp: Int?, isSunrise: Boolean): String {
    if (timestamp == null) return "N/A"

    // Use remember to cache the formatted time based on the timestamp and isSunrise flag
    return remember(timestamp, isSunrise) {
        val date = Date(timestamp.toLong() * 1000)
        val timeWithoutAmPm = hourMinuteFormatter.format(date)

        // Force AM for sunrise, PM for sunset
        if (isSunrise) {
            "$timeWithoutAmPm AM"
        } else {
            "$timeWithoutAmPm PM"
        }
    }
}

// Data class to hold weather metric information
private data class WeatherMetric(
    val icon: Int,
    val value: String,
    val label: String
)

@Composable
private fun MetricsRow(
    metrics: List<WeatherMetric>,
    fillEmptySpace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        metrics.forEach { metric ->
            WeatherMetricItem(
                icon = metric.icon,
                value = metric.value,
                label = metric.label,
                modifier = Modifier.weight(1f)
            )
        }

        // Add empty space if needed
        if (fillEmptySpace && metrics.size < 3) {
            repeat(3 - metrics.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
