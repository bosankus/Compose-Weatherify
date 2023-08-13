package bose.ankush.weatherify.presentation.home

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.data.room.WeatherEntity
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.presentation.UIState
import bose.ankush.weatherify.presentation.home.component.BriefAirQualityReportCardLayout
import bose.ankush.weatherify.presentation.home.component.CurrentWeatherReportLayout
import bose.ankush.weatherify.presentation.home.component.DailyWeatherForecastReportLayout
import bose.ankush.weatherify.presentation.home.component.HourlyWeatherForecastReportLayout
import bose.ankush.weatherify.presentation.home.state.ShowError
import bose.ankush.weatherify.presentation.home.state.ShowLoading
import bose.ankush.weatherify.presentation.navigation.AppBottomBar
import bose.ankush.weatherify.presentation.navigation.Screen

@Composable
fun HomeScreen(
    viewModel: WeatherViewModel,
    navController: NavController
) {
    val context: Context = LocalContext.current
    val weatherReports: UIState<WeatherEntity> = viewModel.weatherReport.collectAsState().value
    val airQualityReports: UIState<AirQuality> = viewModel.airQualityReport.collectAsState().value

    // reacting as per response state change
    if (weatherReports.isLoading || airQualityReports.isLoading) {
        // Screen loading handler
        HandleScreenLoading()
    }
    if (weatherReports.error?.asString(context).isNullOrEmpty() ||
        weatherReports.error?.asString(context).isNullOrEmpty()
    ) {
        // Screen error handler
        HandleScreenError(
            context,
            weatherReports.error,
            airQualityReports.error
        ) { viewModel.performInitialDataLoading() }
    }

    // Show data on UI
    ShowUIContainer(
        weatherReports.data,
        airQualityReports.data,
        navController
    )


    // Handle back button press to exit app
    BackHandler {
        (context as? Activity)?.finish()
    }
}

@Composable
fun HandleScreenLoading() {
    ShowLoading(modifier = Modifier.fillMaxSize())
}

@Composable
fun HandleScreenError(
    context: Context,
    weatherReports: UiText?,
    airQualityReports: UiText?,
    onErrorAction: () -> Unit
) {
    ShowError(
        modifier = Modifier.fillMaxSize(),
        msg =
        weatherReports?.asString(context) ?: airQualityReports?.asString(context),
        buttonText = stringResource(id = R.string.retry_btn_txt),
        buttonAction = onErrorAction
    )
}

@Composable
private fun ShowUIContainer(
    weatherReports: WeatherEntity?,
    airQualityReports: AirQuality?,
    navController: NavController
) {
    Box {
        Scaffold(content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding, verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TODO: Show custom top app bar with future decided icons

                // Show current weather report
                item {
                    weatherReports?.current?.let { CurrentWeatherReportLayout(it) }
                }

                // Show brief air quality report
                item {
                    airQualityReports?.let {
                        BriefAirQualityReportCardLayout(airQualityReports) {
                            navController.navigate(Screen.AirQualityDetailsScreen.route)
                        }
                    }
                }

                // Show hourly weather forecast report
                item {
                    weatherReports?.hourly?.let { HourlyWeatherForecastReportLayout(it) }
                }

                // Show next 8 day's weather forecast report
                weatherReports?.daily?.let { list ->
                    items(list.size) { DailyWeatherForecastReportLayout(list, it) }
                }
            }
        }, bottomBar = {
            AppBottomBar(
                isVisible = rememberSaveable { mutableStateOf(true) },
                navController = navController
            )
        })
    }
}
