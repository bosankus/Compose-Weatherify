package bose.ankush.home.presentation.home.component

import androidx.compose.runtime.Composable
import bose.ankush.home.domain.model.WeatherForecast

@Composable
internal fun WeatherAlertLayout(
    alerts: List<WeatherForecast.Alert?>?,
    onReadMoreClick: (() -> Unit)? = null,
) {
    if (alerts.isNullOrEmpty()) return

    val firstAlert = alerts.firstOrNull() ?: return

    WeatherAlertCard(
        title = firstAlert.event,
        description = firstAlert.description,
        startTime = firstAlert.start,
        endTime = firstAlert.end,
        source = firstAlert.sender_name,
        onReadMoreClick = onReadMoreClick,
    )
}
