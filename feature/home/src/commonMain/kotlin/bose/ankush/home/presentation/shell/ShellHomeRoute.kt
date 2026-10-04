package bose.ankush.home.presentation.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.presentation.HomeIntent
import bose.ankush.home.presentation.HomeState
import bose.ankush.home.presentation.HomeViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Composable
fun ShellHomeRoute(
    places: @Composable () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val shellViewModel = koinViewModel<ShellViewModel>()
    val home by homeViewModel.state.collectAsStateWithLifecycle()
    val shell by shellViewModel.state.collectAsStateWithLifecycle()
    val placeName = rememberPlaceName(home)

    LaunchedEffect(Unit) {
        shellViewModel.refreshAccount()
    }
    LaunchedEffect(home.userLocation) {
        val location = home.userLocation ?: return@LaunchedEffect
        shellViewModel.onIntent(ShellIntent.LocationUpdated(location.first, location.second))
    }
    LaunchedEffect(placeName) {
        shellViewModel.onIntent(ShellIntent.PlaceNameUpdated(placeName))
    }
    LaunchedEffect(shellViewModel) {
        shellViewModel.effect.collect { effect ->
            when (effect) {
                ShellEffect.OpenSettings -> onOpenSettings()
                ShellEffect.ReloadForecast -> homeViewModel.processIntent(HomeIntent.Refresh)
            }
        }
    }

    val today =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    val hasCurrent = home.weatherData?.current != null
    val forecastStatus =
        forecastSectionStatus(
            isLoading = home.isLoading,
            hasCurrent = hasCurrent,
            hasFailure = home.error != null || home.isOffline || !home.offlineMessage.isNullOrBlank(),
        )
    val forecast =
        home.weatherData
            ?.takeIf { hasCurrent }
            ?.toShellForecast(placeName, today)

    ShellHomeScreen(
        state = shell,
        forecast = forecast,
        forecastStatus = forecastStatus,
        actions =
            ShellActions(
                onIntent = shellViewModel::onIntent,
                onSave = shellViewModel::submitCreate,
                onRetryForecast = { homeViewModel.processIntent(HomeIntent.Refresh) },
                onRetryAccount = shellViewModel::refreshAccount,
                onRetryEvents = shellViewModel::retryEvents,
                onRetrySavedPlace = shellViewModel::retrySavedPlace,
            ),
        places = places,
    )
}

@Composable
private fun rememberPlaceName(state: HomeState): String? {
    val override = state.activeLocationName?.takeIf { it.isNotBlank() }
    val geocoder = koinInject<HomeGeocoder>()
    val location = state.userLocation
    var geocoded by remember(location) { mutableStateOf<String?>(null) }
    LaunchedEffect(location) {
        geocoded =
            if (location == null) {
                null
            } else {
                geocoder.reverseGeocode(location.first, location.second)?.takeIf { it.isNotBlank() }
            }
    }
    return override ?: geocoded
}
