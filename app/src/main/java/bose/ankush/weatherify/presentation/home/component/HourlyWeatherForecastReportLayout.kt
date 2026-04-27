package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.sunriseui.components.WeatherHourCard
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.DateTimeUtils.toFormattedTime
import bose.ankush.weatherify.base.common.Extension.formatTextCapitalization
import bose.ankush.weatherify.base.common.Extension.getIconUrl
import bose.ankush.weatherify.base.common.Extension.toCelsius
import bose.ankush.weatherify.base.common.Extension.wrapText
import bose.ankush.weatherify.domain.model.WeatherForecast
import coil.compose.AsyncImage

@Composable
internal fun HourlyWeatherForecastReportLayout(
    hourlyWeatherForecasts: List<WeatherForecast.Hourly?>
) {
    if (hourlyWeatherForecasts.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.hourly_forecast_heading_txt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    FutureForecastListItem(hourlyWeatherForecasts) { /* Item click action will be implemented in future */ }
                }
            }
        }
    } else {
        // Return empty content when no data is available
    }
}


@Composable
private fun FutureForecastListItem(
    weatherForecast: List<WeatherForecast.Hourly?>,
    onItemClick: (Int) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }

    // Limit the number of items to display for better performance
    val limitedForecast = remember(weatherForecast) {
        weatherForecast.take(24) // Show only 24 hours
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp),
        state = rememberLazyListState() // Add state to prevent unnecessary recompositions
    ) {
        items(
            items = limitedForecast,
            key = { item -> item?.dt ?: 0 } // Use unique key for each item
        ) { item ->
            val index = limitedForecast.indexOf(item)
            val isSelected = selectedItem == index

            val time = item?.dt?.toFormattedTime() ?: stringResource(id = R.string.not_available)
            val temperature = stringResource(
                id = R.string.celsius,
                item?.temp?.toCelsius() ?: stringResource(id = R.string.not_available)
            )

            val firstWeather = item?.weather?.firstOrNull()
            val description =
                (firstWeather?.description ?: stringResource(id = R.string.not_available))
                    .wrapText().formatTextCapitalization()
            val weatherIconUrl = firstWeather?.icon?.getIconUrl()

            WeatherHourCard(
                time = time,
                temperature = temperature,
                weatherDescription = description,
                isSelected = isSelected,
                onClick = {
                    selectedItem = index
                    onItemClick(index)
                },
                iconContent = {
                    AsyncImage(
                        modifier = Modifier.size(40.dp),
                        model = weatherIconUrl,
                        error = painterResource(id = R.drawable.ic_sunny),
                        contentDescription = stringResource(id = R.string.weather_icon_content),
                    )
                }
            )
        }
    }
}
