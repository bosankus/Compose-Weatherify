package bose.ankush.home.presentation.wander

import bose.ankush.home.domain.model.WeatherForecast

/**
 * What one compact alert row on the Wander home shows: the event name (or a fixed fallback, never
 * the description) and the formatted start time. [alert] is what a tap passes to `onOpenAlert`.
 */
internal data class WanderAlertRowContent(
    val alert: WeatherForecast.Alert,
    val title: String,
    val startText: String?,
)

/** One row per alert that has an event name or a description, in the order they arrived. */
internal fun List<WeatherForecast.Alert?>.toWanderAlertRows(): List<WanderAlertRowContent> =
    mapNotNull { alert ->
        alert
            ?.takeIf { !it.event.isNullOrBlank() || !it.description.isNullOrBlank() }
            ?.toWanderAlertRow()
    }

internal fun WeatherForecast.Alert.toWanderAlertRow(): WanderAlertRowContent =
    WanderAlertRowContent(
        alert = this,
        title = event?.takeIf { it.isNotBlank() } ?: ALERT_ROW_FALLBACK_TITLE,
        startText = start?.toIssuedLabel(),
    )

internal const val ALERT_ROW_FALLBACK_TITLE = "Weather alert"
