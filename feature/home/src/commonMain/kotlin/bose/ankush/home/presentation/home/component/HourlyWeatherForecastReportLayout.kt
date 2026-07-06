package bose.ankush.home.presentation.home.component

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
import androidx.compose.ui.unit.dp
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.celsius
import bose.ankush.home.generated.resources.hourly_forecast_heading_txt
import bose.ankush.home.generated.resources.ic_sunny
import bose.ankush.home.generated.resources.not_available
import bose.ankush.home.generated.resources.weather_icon_content
import bose.ankush.home.presentation.home.util.formatTextCapitalization
import bose.ankush.home.presentation.home.util.getIconUrl
import bose.ankush.home.presentation.home.util.toCelsius
import bose.ankush.home.presentation.home.util.toFormattedTime
import bose.ankush.home.presentation.home.util.wrapText
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HourlyWeatherForecastReportLayout(hourlyWeatherForecasts: List<WeatherForecast.Hourly?>) {
    if (hourlyWeatherForecasts.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.hourly_forecast_heading_txt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )

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
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                ) {
                    FutureForecastListItem(hourlyWeatherForecasts) {}
                }
            }
        }
    }
}

@Composable
private fun FutureForecastListItem(
    weatherForecast: List<WeatherForecast.Hourly?>,
    onItemClick: (Int) -> Unit,
) {
    var selectedItem by remember { mutableStateOf(0) }

    val limitedForecast = remember(weatherForecast) { weatherForecast.take(24) }
    val notAvailable = stringResource(Res.string.not_available)
    val weatherIconContentDesc = stringResource(Res.string.weather_icon_content)

    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 16.dp),
        state = rememberLazyListState(),
    ) {
        items(
            items = limitedForecast,
            key = { item -> item?.dt ?: 0 },
        ) { item ->
            val index = limitedForecast.indexOf(item)
            val isSelected = selectedItem == index

            val time = item?.dt?.toFormattedTime() ?: notAvailable
            val temperature = stringResource(Res.string.celsius, item?.temp?.toCelsius() ?: notAvailable)

            val firstWeather = item?.weather?.firstOrNull()
            val description = (firstWeather?.description ?: notAvailable).wrapText().formatTextCapitalization()
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
                        error = painterResource(Res.drawable.ic_sunny),
                        contentDescription = weatherIconContentDesc,
                    )
                },
            )
        }
    }
}
