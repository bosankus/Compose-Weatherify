package bose.ankush.weatherify.presentation.details.component

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
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.presentation.details.DetailsViewModel
import bose.ankush.weatherify.presentation.ui.theme.TextWhite
import com.airbnb.lottie.compose.*

@Composable
fun TodaysForecastLayout(
    viewModel: DetailsViewModel,
    modifier: Modifier
) {
    Box(
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Show location details
            LocationNameSection(viewModel)

            // Show animated condition of cloud using Lottie
            CloudConditionAnimatedLayout()

            // Show today's current temperature & weather state
            CurrentTemperatureInCelsius(viewModel)

            // Show Wind speed, humidity, and some other feature in row
        }
    }
}


@Composable
fun LocationNameSection(viewModel: DetailsViewModel) {
    val cityName = viewModel.cityName.value
    Row(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_location),
            tint = Color.White,
            contentDescription = stringResource(id = R.string.location_icon_content)
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = cityName ?: "--",
            style = MaterialTheme.typography.h6,
            color = TextWhite
        )
    }
}


@Composable
fun CloudConditionAnimatedLayout() {
    val compositionResult: LottieCompositionResult =
        rememberLottieComposition(
            spec = LottieCompositionSpec.Asset(
                "sunny_snowfall.json"
            )
        )

    val progress by animateLottieCompositionAsState(
        composition = compositionResult.value,
        isPlaying = true,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = compositionResult.value, progress = progress,
        modifier = Modifier.padding(all = 20.dp).size(120.dp)
    )
}


@Composable
fun CurrentTemperatureInCelsius(viewModel: DetailsViewModel) {
    Text(
        modifier = Modifier.padding(top = 10.dp),
        text = stringResource(id = R.string.celsius, "21"),
        style = MaterialTheme.typography.h1,
        color = Color.White
    )
}