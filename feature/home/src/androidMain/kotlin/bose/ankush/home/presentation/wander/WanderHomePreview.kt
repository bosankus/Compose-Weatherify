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
            feel = "14°",
            wind = "8 km/h",
            uv = "1",
        )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, backgroundColor = 0xFF101418)
@Composable
internal fun WanderHomeScreenPreview() {
    WanderHomeScreen(mock = WanderHomePreviewData.home)
}
