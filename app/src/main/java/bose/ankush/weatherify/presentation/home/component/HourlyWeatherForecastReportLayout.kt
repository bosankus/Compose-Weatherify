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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import bose.ankush.weatherify.data.room.weather.WeatherEntity
import coil.compose.AsyncImage

@Composable
internal fun HourlyWeatherForecastReportLayout(
    hourlyWeatherForecasts: List<WeatherEntity.Hourly?>
) {
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
        if (hourlyWeatherForecasts.isNotEmpty()) {
            FutureForecastListItem(hourlyWeatherForecasts) { /*TODO: item on click action*/ }
        }

    }
}


@Composable
private fun FutureForecastListItem(
    weatherForecast: List<WeatherEntity.Hourly?>,
    onItemClick: (Int) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp)
    ) {
        items(weatherForecast.size) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        selectedItem = it
                        onItemClick(it)
                    }
                    .background(
                        if (selectedItem == it) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = weatherForecast[it]?.dt?.toFormattedTime()
                            ?: stringResource(id = R.string.not_available),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alpha(0.6f),
                    )
                    AsyncImage(
                        model = weatherForecast[it]?.weather?.get(0)?.icon?.getIconUrl(),
                        placeholder = painterResource(id = R.drawable.ic_sunny),
                        contentDescription = stringResource(id = R.string.weather_icon_content),
                    )
                    Text(
                        text = stringResource(
                            id = R.string.celsius,
                            weatherForecast[it]?.temp?.toCelsius()
                                ?: stringResource(id = R.string.not_available)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = (weatherForecast[it]?.weather?.get(0)?.description
                            ?: stringResource(id = R.string.not_available)).wrapText()
                            .formatTextCapitalization(),
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
