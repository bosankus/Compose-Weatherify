package bose.ankush.weatherify.presentation.home.component

import androidx.compose.runtime.Composable
import bose.ankush.sunrisesunset.presentation.SunriseSunsetView
import java.util.Calendar
import java.util.Locale

@Composable
fun SunriseSunsetLayout() {
    SunriseSunsetView(
        sunriseTime = Calendar.getInstance(Locale.getDefault()).apply {
            set(Calendar.SECOND, 0)
            set (Calendar.MINUTE, 5)
            set(Calendar.HOUR_OF_DAY, 52)
        }.timeInMillis,
        sunsetTime = Calendar.getInstance(Locale.getDefault()).apply {
            set(Calendar.SECOND, 0)
            set (Calendar.MINUTE, 17)
            set(Calendar.HOUR_OF_DAY, 52)
        }.timeInMillis,
    )
}