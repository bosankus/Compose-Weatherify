package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.Extension.toCelsius
import bose.ankush.weatherify.presentation.home.HomeViewModel
import bose.ankush.weatherify.presentation.ui.theme.AppDefaultColor
import bose.ankush.weatherify.presentation.ui.theme.BaseTextColor
import bose.ankush.weatherify.presentation.ui.theme.Black
import bose.ankush.weatherify.presentation.ui.theme.TextWhite
import com.airbnb.lottie.compose.*
import com.bosankus.utilities.DateTimeUtils
import kotlin.text.Typography

@Composable
fun TodaysForecastLayout(
    viewModel: HomeViewModel = hiltViewModel(),
) {
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
            CurrentTemperatureInCelsius(viewModel)

            // Show Wind speed, humidity, and some other feature in row
        }
    }
}

@Composable
fun CurrentDate() {
    Surface(
        modifier = Modifier.padding(top = 20.dp),
        color = Black,
        shape = RoundedCornerShape(20.dp)
    ) {
        val epoch = DateTimeUtils.getCurrentTimestamp()
        val currentDateTime = remember { DateTimeUtils.getFormattedDateTimeFromEpoch(epoch) }
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
            text = currentDateTime,
            style = MaterialTheme.typography.h4,
            color = AppDefaultColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CurrentTemperatureInCelsius(viewModel: HomeViewModel) {
    val weather = viewModel.todayWeather.value.data

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.padding(top = 20.dp),
            text = stringResource(
                id = R.string.degree,
                weather?.temp?.toCelsius() ?: stringResource(id = R.string.not_available)
            ),
            style = MaterialTheme.typography.h1,
            fontSize = 200.sp,
            color = Black
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_humidity),
                tint = Color.White,
                contentDescription = stringResource(id = R.string.humidity_icon_content)
            )
            Text(
                modifier = Modifier.padding(start = 5.dp),
                text = stringResource(id = R.string.percent, weather?.humidity.toString()),
                style = MaterialTheme.typography.h4,
                color = TextWhite
            )

            Icon(
                modifier = Modifier.padding(start = 20.dp),
                painter = painterResource(id = R.drawable.ic_wind),
                tint = Color.White,
                contentDescription = stringResource(id = R.string.wind_icon_content)
            )
            Text(
                modifier = Modifier.padding(start = 5.dp),
                text = stringResource(id = R.string.speed, weather?.wind.toString()),
                style = MaterialTheme.typography.h4,
                color = TextWhite
            )
        }
    }
}