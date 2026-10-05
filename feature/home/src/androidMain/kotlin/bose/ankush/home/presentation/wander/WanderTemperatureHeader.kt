package bose.ankush.home.presentation.wander

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WanderTemperatureHeader(
    temperature: String,
    place: String,
    conditionLine: String,
    modifier: Modifier = Modifier,
    contentColor: Color = WanderOnDark,
) {
    Column(modifier = modifier) {
        WanderAnimatedValue(
            text = temperature.ifBlank { WANDER_PLACEHOLDER },
            color = contentColor,
            fontSize = 96.sp,
            fontWeight = FontWeight.Light,
            style =
                TextStyle(
                    lineHeight = 96.sp,
                    letterSpacing = (-1.5).sp,
                    shadow = HeaderTextShadow,
                ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        WanderAnimatedValue(
            text = place.ifBlank { WANDER_PLACEHOLDER },
            color = contentColor,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(lineHeight = 35.sp, shadow = HeaderTextShadow),
        )
        Spacer(modifier = Modifier.height(10.dp))
        WanderAnimatedValue(
            text = conditionLine.ifBlank { WANDER_PLACEHOLDER },
            color = contentColor,
            fontSize = 18.sp,
            style = TextStyle(shadow = HeaderTextShadow),
        )
    }
}
