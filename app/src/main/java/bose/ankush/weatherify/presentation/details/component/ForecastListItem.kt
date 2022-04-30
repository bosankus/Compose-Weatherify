package bose.ankush.weatherify.presentation.details.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.data.remote.dto.ForecastDto
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
            .padding(all = 10.dp),
    ) {
        Text(
            text = forecastDto.dt?.let { DateTimeUtils.getDayNameFromEpoch(it) } ?: "--",
            style = MaterialTheme.typography.body1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
        Text(
            text = "56°   78°",
            style = MaterialTheme.typography.body1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_rain),
            contentDescription = "Play icon button",
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
    }
}