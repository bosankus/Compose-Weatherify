package bose.ankush.home.presentation.wander

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
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
        Text(
            text = temperature,
            style =
                TextStyle(
                    color = contentColor,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 96.sp,
                    letterSpacing = (-1.5).sp,
                    shadow = HeaderTextShadow,
                ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = place,
            style =
                TextStyle(
                    color = contentColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 35.sp,
                    shadow = HeaderTextShadow,
                ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = conditionLine,
            style =
                TextStyle(
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    shadow = HeaderTextShadow,
                ),
        )
    }
}
