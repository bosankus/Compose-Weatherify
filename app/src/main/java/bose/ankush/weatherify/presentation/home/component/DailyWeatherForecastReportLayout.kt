package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.components.AnimatedWeatherIcon
import bose.ankush.commonui.components.WeatherDayCard
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.DateTimeUtils.dayName
import bose.ankush.weatherify.base.common.Extension.toCelsius
import bose.ankush.weatherify.domain.model.WeatherForecast

/**
 * This composable is responsible for showing daily weather forecast section on HomeScreen.
 * It displays a heading and a list of daily forecasts.
 */
@Composable
internal fun DailyWeatherForecastReportLayout(list: List<WeatherForecast.Daily?>) {
    if (list.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(id = R.string.daily_forecast_heading_txt),
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

/**
 * This composable is responsible for showing a single daily weather forecast item.
 * Shows the forecast for a specific day including day name, temperature range, and weather icon.
 * Uses the WeatherDayCard from the sunriseui module.
 */
@Composable
internal fun DailyWeatherForecastItem(
    list: List<WeatherForecast.Daily?>,
    item: Int,
) {
    val dayName = list[item]?.dt?.dayName() ?: stringResource(id = R.string.not_available)
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
