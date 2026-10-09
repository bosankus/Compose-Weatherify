package bose.ankush.home.ios

import bose.ankush.home.presentation.screen.SkyCondition
import bose.ankush.home.presentation.screen.TemperatureTrendDirection

/*
 * What the SwiftUI home screen draws. Everything is already formatted and localized here, so
 * Swift only lays it out: the same mapping and copy as the Android screen, shared through
 * commonMain. Lists carry stable string keys for SwiftUI's ForEach.
 */

/** One full frame of the home screen. */
data class HomeScreenUi(
    val strings: HomeStrings,
    val header: HeaderUi,
    val backdrop: BackdropUi,
    val avatarUrl: String?,
    val chrome: ChromeUi,
    val weather: WeatherPageUi,
    val places: PlacesPageUi,
    val search: PlaceSearchUi,
    val composer: EventComposerUi,
)

data class HeaderUi(
    val observedLabel: String?,
    val temperature: String,
    val place: String,
    val conditionLine: String,
    /** Set while a saved place, not GPS, drives the forecast. */
    val locationOverrideName: String?,
    val locationOverrideDescription: String?,
)

data class BackdropUi(
    val condition: SkyCondition,
    val photo: BackdropPhotoUi?,
)

data class BackdropPhotoUi(
    val id: String,
    val imageUrl: String,
    val photographer: String,
    val profileUrl: String,
    val unsplashUrl: String,
)

/** Pull to refresh, background refresh, prompts and banners around the content. */
data class ChromeUi(
    val isRefreshing: Boolean,
    val isBackgroundRefreshing: Boolean,
    val offlineMessage: String?,
    val showNotificationPrompt: Boolean,
    val notificationDeclinedForever: Boolean,
    val gpsDisabled: Boolean,
    val locationPermissionDenied: Boolean,
    val forecastFailed: Boolean,
    val statusMessage: String?,
)

/** First page: every section under the header. Empty or null parts stay off the page. */
data class WeatherPageUi(
    val details: List<DetailCellUi>,
    val report: CurrentReportUi?,
    val trend: TrendUi?,
    val week: WeekUi?,
    val alerts: List<AlertUi>,
    val airQuality: AirQualityUi?,
    val hourly: List<HourUi>,
    val showPromoCards: Boolean,
    val nearby: NearbyUi?,
)

/** Which glyph a detail cell shows; Swift maps it to an SF Symbol. */
enum class DetailKind { FEEL, WIND, UV, HUMIDITY, PRESSURE, CLOUDS, RAIN_TODAY, NEXT_RAIN }

data class DetailCellUi(
    val kind: DetailKind,
    val label: String,
    val value: String,
    val secondary: String?,
)

/** Coarse sky for the report icon. */
enum class SkyGlyph { CLEAR_DAY, CLEAR_NIGHT, CLOUDS, RAIN, SNOW, THUNDER, MIST }

data class CurrentReportUi(
    val condition: String,
    val glyph: SkyGlyph,
    val sunrise: String,
    val sunset: String,
    /** 0 at sunrise, 1 at sunset. */
    val sunProgress: Float,
)

data class TrendUi(
    val line: String,
    val direction: TemperatureTrendDirection,
)

data class WeekUi(
    val days: List<DayUi>,
    val showRetry: Boolean,
)

data class DayUi(
    val key: String,
    val label: String,
    val date: String,
    val isToday: Boolean,
    val hasEvent: Boolean,
    val iconUrl: String?,
    val range: String,
    val caption: String,
)

data class AlertUi(
    val key: String,
    val title: String,
    val startText: String?,
    val summary: String?,
    val duration: String,
    val source: String,
    val sections: List<AlertSectionUi>,
    val fallback: String?,
)

data class AlertSectionUi(
    val key: String,
    val heading: String?,
    val body: String,
    val items: List<String>?,
)

/** OpenWeather band, good to very poor; Swift picks the dot and bar colors. */
enum class AirBand { GOOD, FAIR, MODERATE, POOR, VERY_POOR }

data class AirQualityUi(
    val status: String,
    val aqiLabel: String,
    val band: AirBand,
    val dominant: String?,
    val readings: List<PollutantUi>,
)

data class PollutantUi(
    val key: String,
    val name: String,
    val value: String,
    val unit: String,
    val fraction: Float,
    val band: AirBand,
)

data class HourUi(
    val key: String,
    val time: String,
    val iconUrl: String?,
    val temperature: String,
    val caption: String,
)

data class NearbyUi(
    val showEvents: Boolean,
    val eventsLoading: Boolean,
    val eventsFailed: Boolean,
    val events: List<NearbyEventUi>,
    val savedPlace: FeaturedPlaceUi?,
)

data class NearbyEventUi(
    val key: String,
    val title: String,
    val whenLabel: String,
)

data class FeaturedPlaceUi(
    val name: String,
    val subtitle: String,
)

/** Second page: GPS plus every saved place. */
data class PlacesPageUi(
    val isPremium: Boolean,
    val isLoading: Boolean,
    val loadFailed: Boolean,
    val notice: String?,
    val currentIsActive: Boolean,
    val places: List<PlaceUi>,
    val showAddButton: Boolean,
)

data class PlaceUi(
    val key: String,
    val title: String,
    val detail: String,
    val isActive: Boolean,
    /** Null hides the remove button (a place still being stored). */
    val deleteLabel: String?,
)

/** The add-a-place sheet. The pick lives in the controller, so the button logic stays shared. */
data class PlaceSearchUi(
    val isOpen: Boolean,
    val query: String,
    val results: List<SuggestionUi>,
    val isSearching: Boolean,
    val isSaving: Boolean,
    val status: String?,
    val actionLabel: String,
    val actionEnabled: Boolean,
)

data class SuggestionUi(
    val key: String,
    val name: String,
    val detail: String,
    val isSaved: Boolean,
    val isSelected: Boolean,
)

/** The new-event sheet. Date and time travel as `YYYY-MM-DD` and `HH:mm`. */
data class EventComposerUi(
    val isVisible: Boolean,
    val subtitle: String,
    val title: String,
    val dateText: String,
    val timeText: String,
    val error: String?,
    val isSubmitting: Boolean,
    val canSubmit: Boolean,
    val actionLabel: String,
)

/** One-off requests the Swift side carries out. */
sealed interface HomeScreenEvent {
    data object RequestLocationPermission : HomeScreenEvent

    data object RequestNotificationPermission : HomeScreenEvent

    /** Straight to this app's notification switch in the OS settings. */
    data object OpenNotificationSettings : HomeScreenEvent

    data object OpenLocationSettings : HomeScreenEvent

    /** The active location changed; go back to the weather page. */
    data object ShowWeather : HomeScreenEvent

    /** Another screen asked for saved places. */
    data object ShowPlaces : HomeScreenEvent
}
