package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.DateTimeUtils
import bose.ankush.weatherify.base.common.Extension.toCelsius
import bose.ankush.weatherify.domain.model.Weather

@Composable
internal fun TodayForecastLayout(weather: Weather? = Weather()) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Show today's date
            CurrentDate()

            // Show today's current temperature & weather state
            CurrentTemperatureInCelsius(weather)

            // Show Wind speed, humidity, and some other feature in row
        }
    }
}

@Composable
private fun CurrentDate() {
    Surface(
        modifier = Modifier.padding(top = 20.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        val epoch = DateTimeUtils.getCurrentTimestamp()
        val currentDateTime =
            remember { DateTimeUtils.getFormattedDateTimeFromEpoch(epoch.toLong()) }
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 5.dp),
            text = currentDateTime,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CurrentTemperatureInCelsius(weatherData: Weather?) {
    val weather = remember { weatherData }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.padding(top = 20.dp),
            text = stringResource(
                id = R.string.degree,
                weather?.temp?.toCelsius() ?: stringResource(id = R.string.not_available)
            ),
            style = MaterialTheme.typography.displayLarge,
            fontSize = 150.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_humidity),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(id = R.string.humidity_icon_content)
            )
            Text(
                modifier = Modifier.padding(start = 5.dp),
                text = stringResource(id = R.string.percent, weather?.humidity.toString()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Icon(
                modifier = Modifier.padding(start = 20.dp),
                painter = painterResource(id = R.drawable.ic_wind),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(id = R.string.wind_icon_content)
            )
            Text(
                modifier = Modifier.padding(start = 5.dp),
                text = stringResource(id = R.string.speed, weather?.wind.toString()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Preview
@Composable
private fun TodayForecastLayoutPreview() {
    TodayForecastLayout(Weather())
}