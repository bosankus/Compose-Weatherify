package bose.ankush.weatherify.presentation.details.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.Extension.toCelsius
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import coil.compose.AsyncImage
import com.bosankus.utilities.DateTimeUtils

@Composable
fun DetailedForecastListItem(
    detailedForecastList: List<ForecastDto.ForecastList>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        items(detailedForecastList.size) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 18.dp),
            ) {
                Text(
                    text = detailedForecastList[it].dt?.let { DateTimeUtils.getTimeFromEpoch(it) }
                        ?: "--",
                    style = MaterialTheme.typography.body1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${detailedForecastList[it].main?.tempMax?.toCelsius()}°   ${detailedForecastList[it].main?.tempMin?.toCelsius()}°",
                    style = MaterialTheme.typography.body1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                AsyncImage(
                    model = "https://openweathermap.org/img/wn/" +
                            "${detailedForecastList[it].weather?.get(0)?.icon}@2x.png",
                    contentDescription = stringResource(id = R.string.weather_icon_content),
                )
            }
        }
    }
}