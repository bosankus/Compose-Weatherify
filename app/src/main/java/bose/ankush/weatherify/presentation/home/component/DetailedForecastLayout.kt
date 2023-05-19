package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.DateTimeUtils
import bose.ankush.weatherify.common.Extension.toCelsius
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import coil.compose.AsyncImage

@Composable
fun DetailedForecastLayout(list: List<ForecastDto.ForecastList>, item: Int) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = list[item].dt?.let { DateTimeUtils.getTimeFromEpoch(it) }
                ?: stringResource(id = R.string.not_available),
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${list[item].main?.tempMax?.toCelsius()}°   ${list[item].main?.tempMin?.toCelsius()}°",
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        AsyncImage(
            modifier = Modifier.weight(0.3f),
            model = "https://openweathermap.org/img/wn/" +
                    "${list[item].weather?.get(0)?.icon}@2x.png",
            placeholder = painterResource(id = R.drawable.ic_sunny),
            contentDescription = stringResource(id = R.string.weather_icon_content),
        )
    }
}