package bose.ankush.home.presentation.wander

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Android-only WanderWeather-style home shell. Not wired as the default home route.
 * Metric chips, the calendar strip, and the small cards are separate composables.
 */
@Composable
fun WanderHomeScreen(
    mock: WanderHomeMock = WanderHomePreviewData.home,
    modifier: Modifier = Modifier,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(WanderTab.HOME.name) }
    val selectedTab = WanderTab.entries.firstOrNull { it.name == selectedTabName } ?: WanderTab.HOME

    Box(modifier = modifier.fillMaxSize()) {
        WanderConditionBackground(condition = mock.condition)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            WanderTemperatureHeader(
                temperature = mock.temperature,
                place = mock.place,
                conditionLine = mock.condition.line,
            )
            Spacer(modifier = Modifier.height(28.dp))
            WanderMetricChips(
                feel = mock.feel,
                wind = mock.wind,
                uv = mock.uv,
            )
            Spacer(modifier = Modifier.height(20.dp))
            WanderCalendarStrip()
            Spacer(modifier = Modifier.height(16.dp))
            WanderSmallCards()
            Spacer(modifier = Modifier.weight(1f))
            WanderTabBar(
                selected = selectedTab,
                onSelected = { selectedTabName = it.name },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
