package bose.ankush.weatherify.presentation.home.component

import androidx.compose.runtime.Composable
import bose.ankush.commonui.components.WeatherAlertCard
import bose.ankush.weatherify.domain.model.WeatherForecast

@Composable
fun WeatherAlertLayout(
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
