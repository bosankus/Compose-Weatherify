package bose.ankush.weatherify.wear.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import bose.ankush.weatherify.wear.data.WeatherUiMapper
import bose.ankush.weatherify.wear.presentation.components.horizontalScreenMargin
import bose.ankush.weatherify.wear.presentation.screens.DialScreen
import bose.ankush.weatherify.wear.presentation.screens.HourlyDialScreen
import bose.ankush.weatherify.wear.presentation.screens.TodayGridScreen
import bose.ankush.weatherify.wear.presentation.screens.WeekScreen
import bose.ankush.weatherify.wear.presentation.theme.DialColors
import bose.ankush.weatherify.wear.presentation.theme.WeatherifyWearTheme
import kotlin.math.min

private enum class WeatherPage { DIAL, HOURLY, TODAY, WEEK }

/**
 * The synced forecast as swipeable pages: the dial glance, the next twelve hours, today's
 * details, and the week. Hourly and week only appear when the phone synced that data
 * (both are plan entitlements, so a free account may not have them).
 */
@Composable
internal fun WeatherPagerScreen(
    uiState: WeatherUiState,
    onAlertClick: () -> Unit,
) {
    val pages =
        remember(uiState.hourly.isEmpty(), uiState.daily.size) {
            buildList {
                add(WeatherPage.DIAL)
                if (uiState.hourly.isNotEmpty()) add(WeatherPage.HOURLY)
                add(WeatherPage.TODAY)
                if (uiState.daily.size > 1) add(WeatherPage.WEEK)
            }
        }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    HorizontalPagerScaffold(pagerState = pagerState) {
        HorizontalPager(state = pagerState) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                when (pages[page]) {
                    WeatherPage.DIAL -> DialScreen(uiState = uiState, onAlertClick = onAlertClick)
                    WeatherPage.HOURLY -> HourlyDialScreen(hours = uiState.hourly)
                    WeatherPage.TODAY -> TodayGridScreen(uiState = uiState)
                    WeatherPage.WEEK -> WeekScreen(days = uiState.daily)
                }
            }
        }
    }
}

/** Shown until [bose.ankush.weatherify.wear.data.WeatherSyncStore] has a forecast from the
 * paired phone. There is no on-watch fetch path, so there's nothing to retry here. */
@Composable
internal fun WaitingForSyncScreen() {
    val transition = rememberInfiniteTransition(label = "waiting")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 6000, easing = LinearEasing)),
        label = "waiting-ring",
    )

    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = min(size.width, size.height) / 2f * 0.6f
                rotate(rotation) {
                    drawCircle(
                        color = DialColors.Crimson.copy(alpha = 0.75f),
                        radius = radius,
                        style =
                            Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect =
                                    PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
                            ),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = horizontalScreenMargin() * 2),
            ) {
                Icon(
                    imageVector = Icons.Filled.WbCloudy,
                    contentDescription = null,
                    tint = DialColors.Crimson,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Waiting for your phone",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = DialColors.TextPrimary,
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun WeatherPagerScreenPreview() {
    WeatherifyWearTheme {
        AppScaffold {
            WeatherPagerScreen(
                uiState = WeatherUiMapper.mapToUiState(mockWeatherForecast, MOCK_LOCATION_NAME),
                onAlertClick = {},
            )
        }
    }
}
