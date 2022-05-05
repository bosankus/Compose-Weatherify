package bose.ankush.weatherify.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.Extension.getForecastListForNext4Days
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.presentation.details.component.DetailedForecastListItem
import bose.ankush.weatherify.presentation.details.component.FutureForecastListItem
import bose.ankush.weatherify.presentation.ui.theme.*

private var fourDaysForecasts: List<AvgForecast> = listOf()
private var dayWiseForecastDetails: List<ForecastDto.ForecastList>? = listOf()

@Composable
fun DetailsFragmentScreen(
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var networkState by remember { mutableStateOf(ForecastListState()) }
    networkState = viewModel.state.value

    // 1. provide the whole

    Box(
        modifier = Modifier
            .background(BackgroundGrey)
            .fillMaxSize()
    ) {
        Column {
            // Show location
            LocationNameSection("Kolkata")

            // Show precaution for today text as per weather
            WeatherAlertSection()

            // Show time wise temperature(min, max, feel) in list format
            FourDaysForecastRow(viewModel, networkState.forecasts)

            // Show detailed forecast time wise when any above column item is selected
            /*DetailedForecastList(viewModel)*/
        }

        // Loading screen
        if (networkState.isLoading) Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = AccentColor
            )
        }

        // Error screen
        if (networkState.error != null) Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = checkNotNull(networkState.error).asString(context),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
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
fun WeatherAlertSection(
    heading: String = "Sample heading",
    content: String = "And some little bit of lulu content her eto show the UI and test it. huha!"
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(all = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DefaultCardBackgroundLightGrey)
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
fun FourDaysForecastRow(
    viewModel: DetailsViewModel,
    forecastList: List<ForecastDto.ForecastList>
) {
    fourDaysForecasts = forecastList.getForecastListForNext4Days()
    var selectedDate by remember { mutableStateOf(0) }
    selectedDate = checkNotNull(forecastList[0].dt)
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.forecast_heading_txt),
            style = MaterialTheme.typography.subtitle1,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )
        FutureForecastListItem(fourDaysForecasts) {
            viewModel.getDayWiseDetailedForecast(selectedDate)
        }

    }
}


@Composable
fun DetailedForecastList(
    viewModel: DetailsViewModel
) {
    dayWiseForecastDetails = viewModel.getDayWiseDetailedForecast(fourDaysForecasts[0].date)
    dayWiseForecastDetails?.let { DetailedForecastListItem(it) } ?: DetailedForecastListItem(
        emptyList()
    )
}