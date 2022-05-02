package bose.ankush.weatherify.presentation.details.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.common.Extension.toCelsius
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import coil.compose.AsyncImage
import com.bosankus.utilities.DateTimeUtils

@Composable
fun ForecastHourlyListItem(
    forecastDto: ForecastDto.ForecastList,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp)
            .padding(start = 10.dp, end = 10.dp),
    ) {
        Text(
            text = forecastDto.dt?.let { DateTimeUtils.getDayNameFromEpoch(it) } ?: "--",
            style = MaterialTheme.typography.body1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${forecastDto.main?.tempMax?.toCelsius()}°   ${forecastDto.main?.tempMin?.toCelsius()}°",
            style = MaterialTheme.typography.body1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        AsyncImage(
            model = "https://openweathermap.org/img/wn/${forecastDto.weather?.get(0)?.icon}@2x.png",
            contentDescription = "weather condition icon",
        )
    }
}