package bose.ankush.weatherify.presentation.home

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import bose.ankush.weatherify.navigation.Screen
import bose.ankush.weatherify.presentation.UIState
import bose.ankush.weatherify.presentation.home.component.*
import bose.ankush.weatherify.presentation.home.state.ShowError
import bose.ankush.weatherify.presentation.home.state.ShowLoading
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
        if (state.isLoading) {
            ShowLoading(modifier = Modifier.fillMaxSize())
        }
        // Screen Error state
        else if (!state.error?.asString(context).isNullOrEmpty()) {
            ShowError(
                modifier = Modifier.fillMaxSize(),
                msg = state.error?.asString(context),
                buttonText = stringResource(id = R.string.retry_btn_txt),
                buttonAction = { viewModel.fetchWeatherDetails(cityName) }
            )
        }
        // Screen with data showing on UI state
        else ShowUIContainer(
            navController = navController,
            detailedForecastList = detailedForecastList
        )
    }
    // No internet connectivity
    else ShowError(
        modifier = Modifier.fillMaxSize(),
        msg = stringResource(id = R.string.no_internet_txt),
        buttonText = stringResource(id = R.string.retry_btn_txt),
        buttonAction = { }
    )
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ShowUIContainer(
    navController: NavController,
    detailedForecastList: List<ForecastDto.ForecastList>
) {
    val viewModel: HomeViewModel = hiltViewModel()
    Box {

        LazyColumn {

            // Show header location name
            item { TopScreenLayout(navController = navController) }

            // Show today's forecasts
            item { TodaysForecastLayout() }

            // Show current air quality condition details
            item {
                AirQualityCardLayout { lat, lon ->
                    navController.navigate(Screen.AirQualityDetailsScreen.withArgs(lat, lon))
                }
            }

            // Show next 4 day's average forecast
            item { FourDaysForecastLayout() }

            // TODO: As per current change in the UI scope, this part will be moved to a separate screen.
            // Show next 4 day's hourly forecast
            if (detailedForecastList.isNotEmpty())
                items(detailedForecastList.size) {
                    DetailedForecastLayout(
                        list = detailedForecastList,
                        item = it
                    )
                }
            else {
                val fourDaysForecasts = viewModel.getFourDaysAvgForecast()
                if (fourDaysForecasts.isNotEmpty()) {
                    val dayDate = fourDaysForecasts[0].date
                    dayDate?.let { viewModel.getDayWiseDetailedForecast(dayDate) }
                } else items(0) {
                    DetailedForecastLayout(
                        list = emptyList(),
                        item = it
                    )
                }
            }
        }
    }
}
