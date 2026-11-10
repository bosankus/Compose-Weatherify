package bose.ankush.home.ios

import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.location_override_chip_content_desc
import bose.ankush.home.presentation.HomeState
import bose.ankush.home.presentation.nearby.NearbyState
import bose.ankush.home.presentation.places.HomeProgress
import bose.ankush.home.presentation.places.SavedPlacesState
import bose.ankush.home.presentation.screen.BackgroundPhoto
import bose.ankush.home.presentation.screen.ForecastExtras
import bose.ankush.home.presentation.screen.UNSPLASH_HOME_URL
import bose.ankush.home.presentation.screen.VALUE_PLACEHOLDER
import bose.ankush.home.presentation.screen.forecastZone
import bose.ankush.home.presentation.screen.headerSummaryLine
import bose.ankush.home.presentation.screen.observedLabel
import bose.ankush.home.presentation.screen.placeholderHomeWeatherContent
import bose.ankush.home.presentation.screen.toForecastExtras
import bose.ankush.home.presentation.screen.toHomeWeatherContent
import org.jetbrains.compose.resources.getString

/** Forecast-side inputs of one frame. */
internal class HomeSide(
    val state: HomeState,
    val backdrop: BackdropUi,
)

/** Nearby, account, and saved places inputs of one frame. */
internal class FeatureSide(
    val nearby: NearbyState,
    val avatarUrl: String?,
    val places: SavedPlacesState,
    val pickedSuggestion: String?,
    val onPlacesPage: Boolean,
)

/** Same rules as the Android `HomeScreenRoute` and `rememberHomeScreenChrome`. */
internal suspend fun buildHomeScreenUi(
    strings: HomeStrings,
    homeSide: HomeSide,
    featureSide: FeatureSide,
): HomeScreenUi {
    val state = homeSide.state
    val forecast = state.weatherData
    val current = forecast?.current
    val placeName = state.place?.name
    val live =
        forecast?.takeIf { current != null }?.toHomeWeatherContent(placeName ?: CURRENT_LOCATION)
    val content = live ?: placeholderHomeWeatherContent(place = placeName ?: VALUE_PLACEHOLDER)
    val forecastVisible = live != null
    val forecastFailed =
        !forecastVisible && !state.isLoading && (state.error != null || state.isOffline)
    val overrideName = state.locationChipName
    return HomeScreenUi(
        strings = strings,
        header =
            HeaderUi(
                observedLabel = current?.dt?.let {
                    observedLabel(
                        it,
                        forecastZone(forecast.timezoneOffset)
                    )
                },
                temperature = content.temperature,
                place = content.place,
                conditionLine =
                    headerSummaryLine(
                        summary = forecast?.daily?.firstOrNull()?.summary,
                        description = current?.weather?.firstOrNull()?.description,
                        fallback = content.condition.line,
                    ),
                locationOverrideName = overrideName,
                locationResetting = state.isResettingLocation,
                locationOverrideDescription =
                    overrideName?.let {
                        getString(
                            Res.string.location_override_chip_content_desc,
                            it
                        )
                    },
            ),
        backdrop = homeSide.backdrop,
        avatarUrl = featureSide.avatarUrl,
        chrome =
            ChromeUi(
                isLoading = state.isLoading,
                offlineMessage = state.offlineMessage?.takeIf { forecastVisible && state.isOffline },
                showNotificationPrompt = state.notificationBanner.isVisible,
                notificationDeclinedForever = state.notificationBanner.isPermanentlyDeclined,
                gpsDisabled = state.isGpsDisabled,
                locationPermissionDenied = state.isLocationPermissionDenied,
                forecastFailed = forecastFailed,
                statusMessage = (state.error
                    ?: state.offlineMessage)?.takeIf { !forecastVisible && it.isNotBlank() },
            ),
        weather =
            WeatherPageInput(
                state = state,
                content = content,
                extras = forecast?.toForecastExtras() ?: ForecastExtras(),
                eventDates = featureSide.nearby.eventDates,
                nearby = featureSide.nearby.toNearbyUi(),
            ).toWeatherPageUi(),
        places = featureSide.places.toPlacesPageUi(
            onPlacesPage = featureSide.onPlacesPage,
            mapCenter = state.place?.point,
            home = HomeProgress(
                place = state.place,
                isLoading = state.isLoading,
                failed = state.notice != null
            ),
        ),
        search = featureSide.places.toPlaceSearchUi(featureSide.pickedSuggestion),
        composer = featureSide.nearby.toComposerUi(),
    )
}

internal fun BackgroundPhoto.toUi(): BackdropPhotoUi =
    BackdropPhotoUi(
        id = id,
        imageUrl = imageUrl,
        photographer = photographer,
        profileUrl = profileUrl,
        unsplashUrl = UNSPLASH_HOME_URL,
    )

private const val CURRENT_LOCATION = "Current Location"
