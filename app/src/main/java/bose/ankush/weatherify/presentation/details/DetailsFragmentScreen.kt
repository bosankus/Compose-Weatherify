package bose.ankush.weatherify.presentation.details

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.Extension.toCelsius
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.presentation.details.component.FutureForecastListItem
import bose.ankush.weatherify.presentation.details.component.TodaysForecastLayout
import bose.ankush.weatherify.presentation.details.state.ForecastListState
import bose.ankush.weatherify.presentation.ui.theme.*
import coil.compose.AsyncImage
import com.bosankus.utilities.DateTimeUtils

@Composable
fun DetailsFragmentScreen(
    viewModel: DetailsViewModel
) {
    val context: Context = LocalContext.current
    val state: ForecastListState = viewModel.forecastState.value
    val detailedForecastList = viewModel.detailedForecastState.value


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
        detailedForecastList = detailedForecastList
    )
}


@Composable
fun ShowLoading(
    modifier: Modifier
) {
    Box(modifier = modifier) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(26.dp)
                .align(Alignment.Center),
            color = AccentColor
        )
    }
}


@Composable
fun ShowError(
    error: String?,
    modifier: Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_error),
                contentDescription = "Play icon button",
                modifier = Modifier
                    .size(46.dp)
                    .padding(start = 16.dp),
                tint = RedError
            )
            Text(
                text = error ?: stringResource(id = R.string.no_internet_txt),
                style = MaterialTheme.typography.h3,
                color = TextWhite,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


@Composable
fun ShowUIContainer(
    viewModel: DetailsViewModel,
    detailedForecastList: List<ForecastDto.ForecastList>
) {
    Box(
        modifier = Modifier
            .background(BackgroundGrey)
            .fillMaxSize()
    ) {
        LazyColumn {
            item { LocationNameSection(viewModel = viewModel) }

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


@Composable
fun LocationNameSection(viewModel: DetailsViewModel) {
    TodaysForecastLayout(
        viewModel = viewModel,
        modifier = Modifier.fillMaxWidth()
    )
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
fun FourDaysForecastRow(
    viewModel: DetailsViewModel,
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
        FutureForecastListItem(fourDaysForecasts) {
            val selectedDate = fourDaysForecasts[it].date
            selectedDate?.let { date -> viewModel.getDayWiseDetailedForecast(date) }
        }
    }
}


@Composable
fun DetailedForecastList(list: List<ForecastDto.ForecastList>, item: Int) {
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
                ?: "--",
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

