package bose.ankush.weatherify.presentation.home

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.ConnectivityManager.isNetworkAvailable
import bose.ankush.weatherify.common.DEFAULT_CITY_NAME
import bose.ankush.weatherify.common.Extension.toCelsius
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.presentation.UIState
import bose.ankush.weatherify.presentation.home.component.FutureForecastListItem
import bose.ankush.weatherify.presentation.home.component.ShowError
import bose.ankush.weatherify.presentation.home.component.ShowLoading
import bose.ankush.weatherify.presentation.home.component.TodaysForecastLayout
import bose.ankush.weatherify.presentation.ui.theme.BackgroundGrey
import bose.ankush.weatherify.presentation.ui.theme.DefaultCardBackgroundLightGrey
import bose.ankush.weatherify.presentation.ui.theme.RedError
import bose.ankush.weatherify.presentation.ui.theme.TextWhite
import coil.compose.AsyncImage
import com.bosankus.utilities.DateTimeUtils

@Composable
fun HomeScreen(
    navController: NavController,
    cityName: String = DEFAULT_CITY_NAME,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context: Context = LocalContext.current
    val state: UIState<List<ForecastDto.ForecastList>> = viewModel.forecastState.value
    val detailedForecastList = viewModel.detailedForecastState.value

    BackHandler() {
        (context as? Activity)?.finish()
    }

    // Has internet
    if (isNetworkAvailable(context)) {
        // make network call
        LaunchedEffect(Unit) {
            viewModel.fetchWeatherDetails(cityName)
        }

        // Screen Loading state
        if (state.isLoading) ShowLoading(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGrey)
        )
        // Screen Error state
        else if (!state.error?.asString(context).isNullOrEmpty()) ShowError(
            error = state.error?.asString(context),
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGrey)
        )
        // Screen with data showing on UI state
        else ShowUIContainer(
            viewModel = viewModel,
            navController = navController,
            detailedForecastList = detailedForecastList
        )
    }
    // No internet connectivity
    else ShowError(
        error = stringResource(id = R.string.no_internet_txt),
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGrey)
    )
}


@Composable
private fun ShowUIContainer(
    viewModel: HomeViewModel,
    navController: NavController,
    detailedForecastList: List<ForecastDto.ForecastList>
) {
    Box(
        modifier = Modifier
            .background(BackgroundGrey)
            .fillMaxSize()
    ) {
        LazyColumn {
            item {
                TodaysForecastLayout(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth(),
                    navController = navController
                )
            }

            item { WeatherAlertSection() }

            item { FourDaysForecastRow(viewModel = viewModel) }

            if (detailedForecastList.isNotEmpty())
                items(detailedForecastList.size) {
                    DetailedForecastList(
                        list = detailedForecastList,
                        item = it
                    )
                }
            else {
                val fourDaysForecasts = viewModel.getFourDaysAvgForecast()
                if (fourDaysForecasts.isEmpty())
                    items(emptyList<ForecastDto.ForecastList>().size) {
                        DetailedForecastList(
                            list = emptyList(),
                            item = it
                        )
                    }
                else {
                    val dayDate = fourDaysForecasts[0].date
                    dayDate?.let { viewModel.getDayWiseDetailedForecast(dayDate) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun WeatherAlertSection(
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
                color = Color.White.copy(0.6f),
            )
            Text(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .heightIn(50.dp),
                text = content,
                style = MaterialTheme.typography.body1,
                color = TextWhite,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_alert),
            contentDescription = "Play icon button",
            modifier = Modifier
                .size(46.dp)
                .padding(start = 16.dp),
            tint = RedError
        )
    }
}


@Composable
private fun FourDaysForecastRow(
    viewModel: HomeViewModel,
) {
    val fourDaysForecasts = viewModel.getFourDaysAvgForecast()
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
        FutureForecastListItem(avgForecastList = fourDaysForecasts) {
            val selectedDate = fourDaysForecasts[it].date
            selectedDate?.let { date -> viewModel.getDayWiseDetailedForecast(date) }
        }
    }
}


@Composable
private fun DetailedForecastList(list: List<ForecastDto.ForecastList>, item: Int) {
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
            style = MaterialTheme.typography.body1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${list[item].main?.tempMax?.toCelsius()}°   ${list[item].main?.tempMin?.toCelsius()}°",
            style = MaterialTheme.typography.body1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
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

