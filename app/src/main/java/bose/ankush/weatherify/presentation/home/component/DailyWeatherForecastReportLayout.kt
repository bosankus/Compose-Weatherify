package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.DateTimeUtils.dayName
import bose.ankush.weatherify.base.common.Extension.getIconUrl
import bose.ankush.weatherify.base.common.Extension.toCelsius
import bose.ankush.weatherify.domain.model.WeatherForecast
import coil.compose.AsyncImage

/**
 * This composable is responsible for showing daily weather forecast section on HomeScreen.
 * It displays a heading and a list of daily forecasts.
 */
@Composable
internal fun DailyWeatherForecastReportLayout(list: List<WeatherForecast.Daily?>) {
    if (list.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.daily_forecast_heading_txt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )

            // Use Column instead of LazyColumn to avoid nested scrollable containers
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                list.forEachIndexed { index, _ ->
                    DailyWeatherForecastItem(list, index)
                }
            }
        }
    }
}

/**
 * This composable is responsible for showing a single daily weather forecast item.
 * Shows the forecast for a specific day including day name, temperature range, and weather icon.
 */
@Composable
internal fun DailyWeatherForecastItem(list: List<WeatherForecast.Daily?>, item: Int) {
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
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Day name
                Text(
                    text = list[item]?.dt?.dayName() ?: stringResource(id = R.string.not_available),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Temperature range
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Min temperature
                        Text(
                            text = "${list[item]?.temp?.min?.toCelsius()}°",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Max temperature
                        Text(
                            text = "${list[item]?.temp?.max?.toCelsius()}°",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Optional: Add weather description if available
                    list[item]?.weather?.get(0)?.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Weather icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp),
                        model = list[item]?.weather?.get(0)?.icon?.getIconUrl(),
                        placeholder = painterResource(id = R.drawable.ic_sunny),
                        contentDescription = stringResource(id = R.string.weather_icon_content),
                    )
                }
            }

        }
    }
}
