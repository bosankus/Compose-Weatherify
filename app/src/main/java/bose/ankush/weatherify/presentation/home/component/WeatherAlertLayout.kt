package bose.ankush.weatherify.presentation.home.component

import androidx.compose.runtime.Composable
import bose.ankush.commonui.components.WeatherAlertCard
import bose.ankush.weatherify.domain.model.WeatherForecast

/**
 * This composable is responsible for displaying weather alerts on the HomeScreen.
 * It handles the case when there are no alerts by not rendering anything.
 *
 * @param alerts The list of alerts from the WeatherForecast data
 * @param onReadMoreClick Optional callback for when the "Read More" button is clicked
 */
@Composable
fun WeatherAlertLayout(
    alerts: List<WeatherForecast.Alert?>?,
    onReadMoreClick: (() -> Unit)? = null,
) {
    // If the alerts list is null or empty, don't render anything
    if (alerts.isNullOrEmpty()) {
        return
    }

    // Get the first alert (most recent/important)
    val firstAlert = alerts.firstOrNull() ?: return

    // Render the alert card
    WeatherAlertCard(
        title = firstAlert.event,
        description = firstAlert.description,
        startTime = firstAlert.start,
        endTime = firstAlert.end,
        source = firstAlert.sender_name,
        onReadMoreClick = onReadMoreClick,
    )
}
