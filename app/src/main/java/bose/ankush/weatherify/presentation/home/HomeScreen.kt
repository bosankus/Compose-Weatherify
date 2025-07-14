package bose.ankush.weatherify.presentation.home

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bose.ankush.sunriseui.SunriseSunsetCombinedAnimation
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.presentation.MainViewModel
import bose.ankush.weatherify.presentation.UIState
import bose.ankush.weatherify.presentation.home.component.BriefAirQualityReportCardLayout
import bose.ankush.weatherify.presentation.home.component.CurrentWeatherReportLayout
import bose.ankush.weatherify.presentation.home.component.DailyWeatherForecastReportLayout
import bose.ankush.weatherify.presentation.home.component.HourlyWeatherForecastReportLayout
import bose.ankush.weatherify.presentation.home.state.ShowError
import bose.ankush.weatherify.presentation.home.state.ShowLoading
import bose.ankush.weatherify.presentation.navigation.AppBottomBar
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context: Context = LocalContext.current
    val uiState: UIState = viewModel.uiState.collectAsState().value

    // reacting as per response state change
    when {
        !uiState.error?.asString(context).isNullOrEmpty() -> {
            // Screen error handler
            HandleScreenError(
                context,
                uiState.error
            ) { viewModel.fetchAndSaveLocationCoordinates() }
        }

        uiState.weatherData?.current?.weather?.isNotEmpty() == true ||
                uiState.airQualityData != null -> {
            // Show data on UI
            ShowUIContainer(uiState, navController)
        }

        else -> {
            // Show loading
            HandleScreenLoading()
        }
    }

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
    errorText: UiText?,
    onErrorAction: () -> Unit
) {
    ShowError(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        msg = errorText?.asString(context),
        buttonText = stringResource(id = R.string.retry_btn_txt),
        buttonAction = onErrorAction
    )
}

@Composable
private fun ShowUIContainer(
    uiState: UIState,
    navController: NavController
) {
    val weatherReports = uiState.weatherData
    val airQualityReports = uiState.airQualityData

    // Create transition states for animations
    val currentWeatherTransitionState = remember { MutableTransitionState(false) }
    val airQualityTransitionState = remember { MutableTransitionState(false) }
    val hourlyForecastTransitionState = remember { MutableTransitionState(false) }
    val dailyForecastTransitionState = remember { MutableTransitionState(false) }

    // Start animations with staggered delays when data is loaded
    LaunchedEffect(weatherReports, airQualityReports) {
        // Reset states first
        currentWeatherTransitionState.targetState = false
        airQualityTransitionState.targetState = false
        hourlyForecastTransitionState.targetState = false
        dailyForecastTransitionState.targetState = false

        // Start animations with staggered delays
        delay(100) // Small initial delay
        currentWeatherTransitionState.targetState = true

        delay(200) // Delay for air quality
        airQualityTransitionState.targetState = true

        delay(300) // Delay for hourly forecast
        hourlyForecastTransitionState.targetState = true

        delay(400) // Delay for daily forecast
        dailyForecastTransitionState.targetState = true
    }

    Box {
        // Add the SunriseSunsetCombinedAnimation as a full-screen background
        weatherReports?.current?.let { currentWeather ->
            SunriseSunsetCombinedAnimation(
                sunriseTimestamp = currentWeather.sunrise?.toLong(),
                sunsetTimestamp = currentWeather.sunset?.toLong(),
                currentTimestamp = System.currentTimeMillis() / 1000
            )
        }

        Scaffold(
            containerColor = Color.Transparent, // Make the scaffold background transparent
            content = { innerPadding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // Add state key to prevent unnecessary recompositions
                    state = rememberLazyListState()
                ) {
                    // Show current weather report - prioritize loading this first
                    item(key = "current_weather") {
                        weatherReports?.current?.let {
                            AnimatedVisibility(
                                visibleState = currentWeatherTransitionState,
                                enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                                        slideInVertically(
                                            animationSpec = tween(durationMillis = 500),
                                            initialOffsetY = { it / 3 }
                                        ),
                                exit = fadeOut()
                            ) {
                                CurrentWeatherReportLayout(
                                    it,
                                    uiState.userLocation,
                                    weatherReports.daily?.firstOrNull()?.summary
                                )
                            }
                        }
                    }

                    // Show brief air quality report
                    item(key = "air_quality") {
                        airQualityReports?.let {
                            AnimatedVisibility(
                                visibleState = airQualityTransitionState,
                                enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                                        slideInVertically(
                                            animationSpec = tween(durationMillis = 500),
                                            initialOffsetY = { it / 3 }
                                        ),
                                exit = fadeOut()
                            ) {
                                BriefAirQualityReportCardLayout(airQualityReports, navController)
                            }
                        }
                    }

                    // Show hourly weather forecast report
                    item(key = "hourly_forecast") {
                        weatherReports?.hourly?.let {
                            AnimatedVisibility(
                                visibleState = hourlyForecastTransitionState,
                                enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                                        slideInVertically(
                                            animationSpec = tween(durationMillis = 500),
                                            initialOffsetY = { it / 3 }
                                        ),
                                exit = fadeOut()
                            ) {
                                HourlyWeatherForecastReportLayout(it)
                            }
                        }
                    }

                    // Show next 8 day's weather forecast report
                    item(key = "daily_forecast") {
                        weatherReports?.daily?.let { list ->
                            AnimatedVisibility(
                                visibleState = dailyForecastTransitionState,
                                enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                                        slideInVertically(
                                            animationSpec = tween(durationMillis = 500),
                                            initialOffsetY = { it / 3 }
                                        ),
                                exit = fadeOut()
                            ) {
                                DailyWeatherForecastReportLayout(list)
                            }
                        }
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
