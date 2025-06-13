package bose.ankush.weatherify.presentation.home.component

import android.location.Geocoder
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.DateTimeUtils
import bose.ankush.weatherify.base.common.Extension.formatTextCapitalization
import bose.ankush.weatherify.base.common.Extension.getIconUrl
import bose.ankush.weatherify.base.common.Extension.toCelsius
import bose.ankush.weatherify.domain.model.WeatherForecast
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Combined animation for sunrise and sunset
            SunriseSunsetCombinedAnimation(
                sunriseTimestamp = weatherData.sunrise,
                sunsetTimestamp = weatherData.sunset,
                currentTimestamp = System.currentTimeMillis() / 1000
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Times display
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = formatTimeWithAmPm(weatherData.sunrise, true), // Force AM for sunrise
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

@Composable
private fun SunriseSunsetCombinedAnimation(
    sunriseTimestamp: Int?,
    sunsetTimestamp: Int?,
    currentTimestamp: Long
) {
    // Create a box to contain the animation
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Handle null timestamps
        if (sunriseTimestamp == null || sunsetTimestamp == null) {
            // If timestamps are null, just show a static sun in the middle
            Icon(
                painter = painterResource(id = R.drawable.ic_sunny),
                contentDescription = "Sun",
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(28.dp)
            )
            return@Box
        }

        // Convert timestamps to Long for calculations
        val sunrise = sunriseTimestamp.toLong()
        val sunset = sunsetTimestamp.toLong()
        val current = currentTimestamp

        // Calculate the normalized position (0 to 1) based on current time
        val dayDuration = sunset - sunrise
        val timeElapsed = current - sunrise
        val normalizedTimePosition = (timeElapsed.toFloat() / dayDuration).coerceIn(0f, 1f)

        // Determine if it's before sunrise, after sunset, or during the day
        val isBeforeSunrise = current < sunrise
        val isAfterSunset = current > sunset

        // Create a state to track initial animation progress
        var initialAnimationPlayed by remember { mutableStateOf(false) }

        // Create animation value for initial animation (from sunrise to current position)
        val initialAnimationProgress = remember { androidx.compose.animation.core.Animatable(0f) }

        // Create animation value for continuous movement
        val continuousAnimationValue = remember { androidx.compose.animation.core.Animatable(0f) }

        // Launch the initial animation once when the composable is first displayed
        LaunchedEffect(Unit) {
            if (!initialAnimationPlayed && !isBeforeSunrise && !isAfterSunset) {
                // Animate from sunrise to current position
                initialAnimationProgress.animateTo(
                    targetValue = normalizedTimePosition,
                    animationSpec = tween(durationMillis = 1500)
                )
                initialAnimationPlayed = true

                // Start continuous animation for real-time movement
                continuousAnimationValue.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = ((sunset - current) * 1000 / (24 * 60 * 60)).toInt().coerceAtLeast(1),
                        easing = LinearEasing
                    )
                )
            }
        }

        // Calculate the final progress value
        val progress = when {
            isBeforeSunrise -> 0f // Before sunrise, sun is at the left
            isAfterSunset -> 1f // After sunset, sun is at the right
            !initialAnimationPlayed -> 0f // Animation hasn't started yet
            initialAnimationPlayed -> {
                // Blend between initial animation and continuous animation
                normalizedTimePosition + (continuousAnimationValue.value * (1 - normalizedTimePosition))
            }
            else -> normalizedTimePosition // Fallback to current time position
        }

        // Calculate x position (left to right)
        val xOffset = (progress * 240f - 120f).dp

        // Calculate y position (inverted U shape)
        // Using a parabola: y = -a(x^2) + b where a controls the steepness and b the height
        // Normalize x to be between -1 and 1 for the parabola calculation
        val normalizedX = progress * 2f - 1f
        val yOffset = (-60f * (normalizedX * normalizedX) + 60f).dp

        // Draw the visible path with faded ends using Box composables
        // Create a row of small boxes to form the path
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Create 100 small boxes to form the path
            for (i in 0..100) {
                val normalizedPathX = i / 50f - 1f // -1 to 1

                // Calculate alpha based on position (fade at ends)
                val alpha = when {
                    i < 10 -> (i / 10f) * 0.2f
                    i > 90 -> ((100 - i) / 10f) * 0.2f
                    else -> 0.2f
                }

                // Calculate height based on parabolic function
                val heightPercent = 1 - (normalizedPathX * normalizedPathX)
                val boxHeight = (60 * heightPercent).dp

                // Create a small box for this segment of the path
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(boxHeight)
                        .width(1.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.toFloat())
                        )
                )
            }
        }

        // Draw the sun icon
        Icon(
            painter = painterResource(id = R.drawable.ic_sunny),
            contentDescription = "Sun",
            tint = if (progress < 0.5f) Color(0xFFFFB74D) else Color(0xFFFF7043), // Transition from sunrise to sunset color
            modifier = Modifier
                .size(28.dp)
                .offset(x = xOffset, y = -yOffset) // Negative y because in Compose, y increases downward
        )
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
