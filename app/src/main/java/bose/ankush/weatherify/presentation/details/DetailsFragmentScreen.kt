package bose.ankush.weatherify.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.presentation.details.component.ForecastItem
import bose.ankush.weatherify.presentation.ui.theme.BackgroundGrey
import bose.ankush.weatherify.presentation.ui.theme.ButtonBlue
import bose.ankush.weatherify.presentation.ui.theme.CardBackgroundLightGrey
import bose.ankush.weatherify.presentation.ui.theme.TextWhite

@Composable
fun DetailsFragmentScreen(
    viewModel: DetailsViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier
            .background(BackgroundGrey)
            .fillMaxSize()
    ) {
        Column {
            // Show location
            LocationNameSection("Kolkata")

            // Show precaution for today text as per weather
            WeatherPrecautionSection()

            // Show time wise temperature(min, max, feel) in list format
            AllTimeForecastList(viewModel)
        }
    }
}


@Composable
fun LocationNameSection(name: String) {
    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.h6,
            color = TextWhite
        )
    }
}


@Composable
fun WeatherPrecautionSection(
    heading: String = "Sample heading",
    content: String = "And some little bit of lulu content her eto show the UI and test it. huha!"
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(all = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackgroundLightGrey)
            .padding(horizontal = 15.dp, vertical = 20.dp)
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(weight = 1f, fill = false)
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.body2,
                color = Color.White.copy(0.6f)
            )
            Text(
                modifier = Modifier.padding(top = 3.dp),
                text = content,
                style = MaterialTheme.typography.body1,
                color = TextWhite,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(ButtonBlue)
                .padding(3.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_rain),
                contentDescription = "Play icon button",
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
    }
}


@Composable
fun AllTimeForecastList(viewModel: DetailsViewModel) {
    val state = viewModel.state.value
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(state.forecasts) { forecast -> ForecastItem(forecastDto = forecast) }
    }
}