package bose.ankush.weatherify.wear.presentation

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import bose.ankush.weatherify.wear.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Animated dotLottie asset for a condition, respecting the day phase where a night
 * variant exists. Null means "no animation yet" and the caller falls back to the
 * static [toImageVector] icon — currently SNOW (no asset) and UNKNOWN (deliberate).
 */
@RawRes
internal fun WeatherIconType.toLottieRes(phase: DayPhase): Int? =
    when (this) {
        WeatherIconType.CLEAR ->
            if (phase == DayPhase.NIGHT) R.raw.weather_clear_night else R.raw.weather_clear_day

        WeatherIconType.CLOUDS ->
            if (phase == DayPhase.NIGHT) R.raw.weather_cloudy_night else R.raw.weather_partly_cloudy

        WeatherIconType.RAIN -> R.raw.weather_partly_shower
        WeatherIconType.THUNDERSTORM -> R.raw.weather_storm
        WeatherIconType.ATMOSPHERE -> R.raw.weather_foggy
        WeatherIconType.WIND -> R.raw.weather_windy
        WeatherIconType.SNOW, WeatherIconType.UNKNOWN -> null
    }

/**
 * The res is a dotLottie (zip) asset; Lottie auto-detects the format from the zip magic
 * bytes, no special spec needed beyond RawRes.
 */
@Composable
internal fun WeatherOverlay(
    @RawRes res: Int,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(res))
    val progress by
    animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.fillMaxSize(),
    )
}
