package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.DateTimeUtils.toFormattedTime
import bose.ankush.weatherify.base.common.Extension.formatTextCapitalization
import bose.ankush.weatherify.base.common.Extension.getIconUrl
import bose.ankush.weatherify.base.common.Extension.toCelsius
import bose.ankush.weatherify.base.common.Extension.wrapText
import bose.ankush.weatherify.domain.model.WeatherForecast
import coil.compose.AsyncImage
import coil.request.ImageRequest

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

    // Pre-calculate background colors to avoid recalculation during composition
    val selectedBackground = MaterialTheme.colorScheme.primaryContainer
    val unselectedBackground = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)

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

            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        selectedItem = index
                        onItemClick(index)
                    }
                    .background(if (isSelected) selectedBackground else unselectedBackground)
                    .padding(horizontal = 10.dp, vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Time
                    Text(
                        text = item?.dt?.toFormattedTime() ?: stringResource(id = R.string.not_available),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alpha(0.6f),
                    )

                    // Weather icon
                    AsyncImage(
                        model = item?.weather?.get(0)?.icon?.getIconUrl(),
                        error = painterResource(id = R.drawable.ic_sunny),
                        contentDescription = stringResource(id = R.string.weather_icon_content),
                    )

                    // Temperature
                    Text(
                        text = stringResource(
                            id = R.string.celsius,
                            item?.temp?.toCelsius() ?: stringResource(id = R.string.not_available)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    // Weather description
                    Text(
                        text = (item?.weather?.get(0)?.description ?: stringResource(id = R.string.not_available))
                            .wrapText().formatTextCapitalization(),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alpha(0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
