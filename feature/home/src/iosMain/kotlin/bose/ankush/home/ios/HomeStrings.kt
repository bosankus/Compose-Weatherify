package bose.ankush.home.ios

import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.ai_summary_action
import bose.ankush.home.generated.resources.ai_summary_body
import bose.ankush.home.generated.resources.ai_summary_close
import bose.ankush.home.generated.resources.ai_summary_failed
import bose.ankush.home.generated.resources.ai_summary_for_place
import bose.ankush.home.generated.resources.ai_summary_loading
import bose.ankush.home.generated.resources.ai_summary_privacy
import bose.ankush.home.generated.resources.ai_summary_retry
import bose.ankush.home.generated.resources.ai_summary_tips_heading
import bose.ankush.home.generated.resources.ai_summary_title
import bose.ankush.home.generated.resources.ai_summary_writing
import bose.ankush.home.generated.resources.cancel_btn_txt
import bose.ankush.home.generated.resources.daily_forecast_heading_txt
import bose.ankush.home.generated.resources.enable_gps_btn_txt
import bose.ankush.home.generated.resources.enable_notification_btn
import bose.ankush.home.generated.resources.event_sheet_close
import bose.ankush.home.generated.resources.event_sheet_title
import bose.ankush.home.generated.resources.event_title_hint
import bose.ankush.home.generated.resources.hourly_forecast_heading_txt
import bose.ankush.home.generated.resources.location_override_reset_btn
import bose.ankush.home.generated.resources.location_permission_settings_steps
import bose.ankush.home.generated.resources.network_unavailable_txt
import bose.ankush.home.generated.resources.notification_permission_declined_txt
import bose.ankush.home.generated.resources.notification_permission_message
import bose.ankush.home.generated.resources.offline_toast_title_txt
import bose.ankush.home.generated.resources.open_settings_btn
import bose.ankush.home.generated.resources.retry_btn_txt
import bose.ankush.home.generated.resources.saved_places_add
import bose.ankush.home.generated.resources.saved_places_add_subtitle
import bose.ankush.home.generated.resources.saved_places_current
import bose.ankush.home.generated.resources.saved_places_current_detail
import bose.ankush.home.generated.resources.saved_places_empty
import bose.ankush.home.generated.resources.saved_places_load_error
import bose.ankush.home.generated.resources.saved_places_loading
import bose.ankush.home.generated.resources.saved_places_premium_body
import bose.ankush.home.generated.resources.saved_places_premium_title
import bose.ankush.home.generated.resources.saved_places_saved_badge
import bose.ankush.home.generated.resources.saved_places_search_clear
import bose.ankush.home.generated.resources.saved_places_search_close
import bose.ankush.home.generated.resources.saved_places_search_hint
import bose.ankush.home.generated.resources.saved_places_title
import bose.ankush.home.generated.resources.saved_places_upgrade
import org.jetbrains.compose.resources.getString

/**
 * Fixed copy for the SwiftUI home, read once from this module's compose resources so iOS shows
 * the same translations as Android. Copy that takes an argument is resolved per item instead.
 */
data class HomeStrings(
    val retry: String,
    val cancel: String,
    val dailyForecast: String,
    val hourlyForecast: String,
    val enableGps: String,
    val requestLocation: String,
    val enableNotifications: String,
    val notificationMessage: String,
    val notificationDeclined: String,
    val openSettings: String,
    val locationDenied: String,
    val offlineTitle: String,
    val networkUnavailable: String,
    val resetToGps: String,
    val eventCalendar: String,
    val addEvent: String,
    val nearbyEvents: String,
    val savedPlace: String,
    val account: String,
    val weatherAlerts: String,
    val airQuality: String,
    val sunrise: String,
    val sunset: String,
    val showAll: String,
    val showLess: String,
    val aiSummary: AiSummaryStrings,
    val places: PlacesStrings,
    val eventSheet: EventSheetStrings,
    val tabs: TabStrings,
)

data class AiSummaryStrings(
    val title: String,
    val body: String,
    val action: String,
    val close: String,
    val forPlace: String,
    val loading: String,
    val writing: String,
    val tipsHeading: String,
    val failed: String,
    val retry: String,
    val privacy: String,
)

data class PlacesStrings(
    val title: String,
    val current: String,
    val currentDetail: String,
    val loading: String,
    val empty: String,
    val loadError: String,
    val premiumTitle: String,
    val premiumBody: String,
    val upgrade: String,
    val add: String,
    val addSubtitle: String,
    val searchHint: String,
    val searchClear: String,
    val searchClose: String,
    val savedBadge: String,
)

data class EventSheetStrings(
    val title: String,
    val close: String,
    val titleHint: String,
    val date: String,
    val time: String,
)

data class TabStrings(
    val home: String,
    val map: String,
    val hub: String,
)

internal suspend fun loadHomeStrings(): HomeStrings =
    HomeStrings(
        retry = getString(Res.string.retry_btn_txt),
        cancel = getString(Res.string.cancel_btn_txt),
        dailyForecast = getString(Res.string.daily_forecast_heading_txt),
        hourlyForecast = getString(Res.string.hourly_forecast_heading_txt),
        enableGps = getString(Res.string.enable_gps_btn_txt),
        requestLocation = "Request location permission",
        enableNotifications = getString(Res.string.enable_notification_btn),
        notificationMessage = getString(Res.string.notification_permission_message),
        notificationDeclined = getString(Res.string.notification_permission_declined_txt),
        openSettings = getString(Res.string.open_settings_btn),
        // iOS can only open the app's own Settings page, so say which row to tap there.
        locationDenied = getString(Res.string.location_permission_settings_steps),
        offlineTitle = getString(Res.string.offline_toast_title_txt),
        networkUnavailable = getString(Res.string.network_unavailable_txt),
        resetToGps = getString(Res.string.location_override_reset_btn),
        // Same fixed English copy as the Android screen, which has no resources for these yet.
        eventCalendar = "Event calendar",
        addEvent = "Add event",
        nearbyEvents = "Nearby events",
        savedPlace = "Saved place",
        account = "Account",
        weatherAlerts = "Weather alerts",
        airQuality = "Air quality",
        sunrise = "Sunrise",
        sunset = "Sunset",
        showAll = "Show all",
        showLess = "Show less",
        aiSummary = loadAiSummaryStrings(),
        places = loadPlacesStrings(),
        eventSheet = loadEventSheetStrings(),
        tabs = TabStrings(home = "Home", map = "Map", hub = "Hub"),
    )

private suspend fun loadAiSummaryStrings() =
    AiSummaryStrings(
        title = getString(Res.string.ai_summary_title),
        body = getString(Res.string.ai_summary_body),
        action = getString(Res.string.ai_summary_action),
        close = getString(Res.string.ai_summary_close),
        // Swift swaps in the place name, so the template keeps the translated wording.
        forPlace = getString(Res.string.ai_summary_for_place, PLACE_TOKEN),
        loading = getString(Res.string.ai_summary_loading),
        writing = getString(Res.string.ai_summary_writing),
        tipsHeading = getString(Res.string.ai_summary_tips_heading),
        failed = getString(Res.string.ai_summary_failed),
        retry = getString(Res.string.ai_summary_retry),
        privacy = getString(Res.string.ai_summary_privacy),
    )

private suspend fun loadPlacesStrings() =
    PlacesStrings(
        title = getString(Res.string.saved_places_title),
        current = getString(Res.string.saved_places_current),
        currentDetail = getString(Res.string.saved_places_current_detail),
        loading = getString(Res.string.saved_places_loading),
        empty = getString(Res.string.saved_places_empty),
        loadError = getString(Res.string.saved_places_load_error),
        premiumTitle = getString(Res.string.saved_places_premium_title),
        premiumBody = getString(Res.string.saved_places_premium_body),
        upgrade = getString(Res.string.saved_places_upgrade),
        add = getString(Res.string.saved_places_add),
        addSubtitle = getString(Res.string.saved_places_add_subtitle),
        searchHint = getString(Res.string.saved_places_search_hint),
        searchClear = getString(Res.string.saved_places_search_clear),
        searchClose = getString(Res.string.saved_places_search_close),
        savedBadge = getString(Res.string.saved_places_saved_badge),
    )

private suspend fun loadEventSheetStrings() =
    EventSheetStrings(
        title = getString(Res.string.event_sheet_title),
        close = getString(Res.string.event_sheet_close),
        titleHint = getString(Res.string.event_title_hint),
        date = "Date",
        time = "Time",
    )

/** Stand-in for the place name in [AiSummaryStrings.forPlace]; the Swift sheet replaces it. */
private const val PLACE_TOKEN = "{place}"
