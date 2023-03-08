package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.Extension.toCelsius
import bose.ankush.weatherify.navigation.Screen
import bose.ankush.weatherify.presentation.home.HomeViewModel
import bose.ankush.weatherify.presentation.ui.theme.TextWhite
import com.airbnb.lottie.compose.*

@Composable
fun TodaysForecastLayout(
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier,
    navController: NavController
) {
    Box(
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Show location details
            LocationNameSection(viewModel, navController)

            // Show animated condition of cloud using Lottie
            CloudConditionAnimatedLayout()

            // Show today's current temperature & weather state
            CurrentTemperatureInCelsius(viewModel)

            // TODO: Show Wind speed, humidity, and some other feature in row
        }
    }
}


@Composable
fun LocationNameSection(viewModel: HomeViewModel, navController: NavController) {
    val cityName = viewModel.cityName.value ?: stringResource(id = R.string.not_available)
    Row(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.CitiesListScreen.route) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_location),
            tint = Color.White,
            contentDescription = stringResource(id = R.string.location_icon_content),
        )
        Text(
            modifier = Modifier.padding(start = 5.dp),
            text = cityName,
            style = MaterialTheme.typography.h4,
            color = TextWhite,
        )
    }
}

@Preview
@Composable
fun CloudConditionAnimatedLayout() {
    val compositionResult: LottieCompositionResult =
        rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(R.raw.sunny_snowfall)
        )

    val progress by animateLottieCompositionAsState(
        composition = compositionResult.value,
        isPlaying = true,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = compositionResult.value, progress = { progress },
        modifier = Modifier
            .padding(all = 20.dp)
            .size(120.dp)
    )
}


@Composable
fun CurrentTemperatureInCelsius(viewModel: HomeViewModel) {
    val weather = viewModel.todayWeather.value.data

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(top = 10.dp),
            text = stringResource(
                id = R.string.celsius,
                weather?.temp?.toCelsius() ?: stringResource(id = R.string.not_available)
            ),
            style = MaterialTheme.typography.h1,
            color = TextWhite
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_humidity),
                tint = Color.White,
                contentDescription = stringResource(id = R.string.location_icon_content)
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
                contentDescription = stringResource(id = R.string.location_icon_content)
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