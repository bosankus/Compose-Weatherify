package bose.ankush.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.daily_forecast_heading_txt
import bose.ankush.home.generated.resources.not_available
import bose.ankush.home.presentation.util.dayName
import bose.ankush.home.presentation.util.toCelsius
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DailyWeatherForecastReportLayout(list: List<WeatherForecast.Daily?>) {
    if (list.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.daily_forecast_heading_txt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )

            // Use Column instead of LazyColumn to avoid nested scrollable containers
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                list.forEachIndexed { index, _ ->
                    DailyWeatherForecastItem(list, index)
                }
            }
        }
    }
}

@Composable
private fun DailyWeatherForecastItem(
    list: List<WeatherForecast.Daily?>,
    item: Int,
) {
    val notAvailable = stringResource(Res.string.not_available)
    val dayName = list[item]?.dt?.dayName() ?: notAvailable
    val minTemperature = "${list[item]?.temp?.min?.toCelsius()}°"
    val maxTemperature = "${list[item]?.temp?.max?.toCelsius()}°"

    val firstWeather = list[item]?.weather?.firstOrNull()
    val weatherDescription = firstWeather?.description

    WeatherDayCard(
        dayName = dayName,
        minTemperature = minTemperature,
        maxTemperature = maxTemperature,
        weatherDescription = weatherDescription,
        iconContent = {
            AnimatedWeatherIcon(
                weatherDescription = weatherDescription,
                modifier = Modifier.padding(4.dp),
            )
        },
    )
}
