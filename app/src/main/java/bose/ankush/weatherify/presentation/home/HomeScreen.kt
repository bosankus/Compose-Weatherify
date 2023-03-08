package bose.ankush.weatherify.presentation.home

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.ConnectivityManager.isNetworkAvailable
import bose.ankush.weatherify.common.DEFAULT_CITY_NAME
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.presentation.UIState
import bose.ankush.weatherify.presentation.home.component.AirQualityLayout
import bose.ankush.weatherify.presentation.home.component.DetailedForecastLayout
import bose.ankush.weatherify.presentation.home.component.FourDaysForecastLayout
import bose.ankush.weatherify.presentation.home.component.TodaysForecastLayout
import bose.ankush.weatherify.presentation.home.state.ShowError
import bose.ankush.weatherify.presentation.home.state.ShowLoading
import bose.ankush.weatherify.presentation.ui.theme.BackgroundGrey
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@Composable
fun HomeScreen(
    navController: NavController,
    cityName: String = DEFAULT_CITY_NAME,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context: Context = LocalContext.current
    val state: UIState<List<ForecastDto.ForecastList>> = viewModel.forecastState.value
    val detailedForecastList = viewModel.detailedForecastState.value

    BackHandler {
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


@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ShowUIContainer(
    viewModel: HomeViewModel = hiltViewModel(),
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
                    modifier = Modifier.fillMaxWidth(),
                    navController = navController
                )
            }

            item { AirQualityCardLayout() }

            item { FourDaysForecastLayout() }

            if (detailedForecastList.isNotEmpty())
                items(detailedForecastList.size) {
                    DetailedForecastLayout(
                        list = detailedForecastList,
                        item = it
                    )
                }
            else {
                val fourDaysForecasts = viewModel.getFourDaysAvgForecast()
                if (fourDaysForecasts.isEmpty())
                    items(emptyList<ForecastDto.ForecastList>().size) {
                        DetailedForecastLayout(
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
