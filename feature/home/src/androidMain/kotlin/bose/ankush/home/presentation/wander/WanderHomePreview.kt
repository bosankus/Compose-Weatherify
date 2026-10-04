package bose.ankush.home.presentation.wander

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

data class WanderHomeMock(
    val temperature: String,
    val place: String,
    val condition: WanderCondition,
    val feel: String,
    val wind: String,
    val uv: String,
)

object WanderHomePreviewData {
    val home =
        WanderHomeMock(
            temperature = "16°",
            place = "London",
            condition = WanderCondition.FOG,
            feel = "16°",
            wind = "WSW 6 mph",
            uv = "7",
        )
}

private val previewDays =
    listOf(
        WanderCalendarDay("Sun", "16", "16°", selected = false),
        WanderCalendarDay("Mon", "17", "15°", selected = false),
        WanderCalendarDay("Tue", "18", "14°", selected = false),
        WanderCalendarDay("Wed", "19", "16°", selected = true),
        WanderCalendarDay("Thu", "20", "17°", selected = false),
        WanderCalendarDay("Fri", "21", "18°", selected = false),
        WanderCalendarDay("Sat", "22", "16°", selected = false),
    )

@Preview(showBackground = true, widthDp = 390, heightDp = 844, backgroundColor = 0xFF101418)
@Composable
internal fun WanderHomeScreenPreview() {
    val mock = WanderHomePreviewData.home
    WanderHomeScreen(
        links = WanderHomeLinks(weather = {}, places = {}, onOpenHub = {}),
        shell =
            WanderShell.Ready(
                content =
                    WanderHomeContent(
                        temperature = mock.temperature,
                        place = mock.place,
                        condition = mock.condition,
                        feel = mock.feel,
                        wind = mock.wind,
                        uv = mock.uv,
                        days = previewDays,
                    ),
                fogPhoto = null,
            ),
    )
}
