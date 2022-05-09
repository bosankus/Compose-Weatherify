package bose.ankush.weatherify.presentation.details.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.presentation.details.DetailsViewModel
import bose.ankush.weatherify.presentation.ui.theme.SeaGreen
import bose.ankush.weatherify.presentation.ui.theme.SeaGreenDark
import bose.ankush.weatherify.presentation.ui.theme.TextWhite

@Preview
@Composable
fun TodaysForecastLayout() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        SeaGreen,
                        SeaGreenDark
                    )
                )
            )
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()

        ) {
            // Show location details


            // Show animated condition of cloud using Lottie
            CloudConditionAnimatedLayout()

            // Show today's current temperature & weather state
            Text(
                text = "21",

            )

            // Show Wind speed, humidity, and some other feature in row
        }
    }
}


@Composable
fun LocationNameSection(viewModel: DetailsViewModel) {
    val cityName = viewModel.cityName.value
    Row(
        modifier = Modifier
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
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

}