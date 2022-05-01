package bose.ankush.weatherify.presentation.details.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
fun ForecastItem(
    forecastDto: ForecastDto.ForecastList,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
            .padding(start = 10.dp, end = 10.dp),
    ) {
        Text(
            text = forecastDto.dt?.let { DateTimeUtils.getDayNameFromEpoch(it) } ?: "--",
            style = MaterialTheme.typography.body1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
        Text(
            text = "${forecastDto.main?.tempMax?.toCelsius()}°   ${forecastDto.main?.tempMin?.toCelsius()}°",
            style = MaterialTheme.typography.body1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
        AsyncImage(
            model = "https://openweathermap.org/img/wn/${forecastDto.weather?.get(0)?.icon}@2x.png",
            contentDescription = "weather condition icon"
        )
    }
}