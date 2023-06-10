package bose.ankush.sunrisesunset.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bose.ankush.sunrisesunset.R
import bose.ankush.sunrisesunset.util.getFormattedDate
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Preview(showBackground = true)
@Composable
fun SunriseSunsetView(
    modifier: Modifier = Modifier,
    arcRadius: Dp = 150.dp,
    strokeWidth: Dp = 2.dp,
    animationDuration: Int = 1000,
    animationDelay: Int = 0,
    sunRadius: Float = 50f,
    sunriseText: String = stringResource(id = R.string.sunrise_txt),
    sunriseTextColor: Color = MaterialTheme.colorScheme.onBackground,
    sunriseTimeColor: Color = MaterialTheme.colorScheme.onBackground,
    sunriseTime: Long = Calendar.getInstance(Locale.getDefault()).apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MINUTE, 12)
        set(Calendar.HOUR_OF_DAY, 7)
    }.timeInMillis,
    sunsetText: String = stringResource(id = R.string.sunset_txt),
    sunsetTextColor: Color = MaterialTheme.colorScheme.onBackground,
    sunsetTimeColor: Color = MaterialTheme.colorScheme.onBackground,
    sunsetTime: Long = Calendar.getInstance(Locale.getDefault()).apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MINUTE, 33)
        set(Calendar.HOUR_OF_DAY, 17)
    }.timeInMillis,
    timeFormat: String = "HH:mm",
    arcColorArray: Array<Pair<Float, Color>> = arrayOf(
        0.2f to Color(0xFFBDBCBB),
        0.5f to Color(0xFFE8EBED)
    ),
    sunColorArray: Array<Pair<Float, Color>> = arrayOf(
        0.3f to Color(0xFFF74822),
        0.4f to Color(0xFFF5836A),
        0.9f to Color(0xFFFAF1D1),
        1f to Color(0xFFFFFBEE)
    )
) {
    val currentCalender: Calendar = Calendar.getInstance(Locale.getDefault())
    val currentUnixTime: Long = currentCalender.timeInMillis

    val timeDifference = sunsetTime.minus(sunriseTime)
    val percentage: Float =
        (currentUnixTime.toFloat().minus(sunriseTime.toFloat())).div(timeDifference.toFloat())
    var animationPlayed by rememberSaveable { mutableStateOf(false) }
    val currentPercentage: State<Float> = animateFloatAsState(
        targetValue = if (animationPlayed) percentage else 0f,
        animationSpec = tween(
            durationMillis = animationDuration,
            delayMillis = animationDelay
        ), label = ""
    )

    LaunchedEffect(key1 = true) { animationPlayed = true }

    Box(
        modifier = Modifier.size(arcRadius * 2.5f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(arcRadius * 2)) {
            drawArc(
                brush = Brush.verticalGradient(
                    colorStops = arcColorArray,
                    tileMode = TileMode.Clamp
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round),
                useCenter = false
            )

            val angleInDegrees: Double = if (((currentPercentage.value * 180.0) + 90) > 270) {
                270.0
            } else if (((currentPercentage.value * 180.0) + 90) < 90) {
                90.0
            } else {
                (currentPercentage.value * 180.0) + 90
            }

            val rad = (size.height / 2)
            val x = -(rad * sin(Math.toRadians(angleInDegrees))).toFloat() + (size.width / 2)
            val y = (rad * cos(Math.toRadians(angleInDegrees))).toFloat() + rad

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = sunColorArray,
                    center = Offset(x, y),
                    radius = sunRadius + 10
                ),
                radius = sunRadius,
                center = Offset(x, y)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentWidth()
                .offset(y = 30.dp, x = -arcRadius)
        ) {
            Text(
                text = sunriseText,
                color = sunriseTextColor,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = sunriseTime.getFormattedDate(timeFormat),
                color = sunriseTimeColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentWidth()
                .offset(y = 30.dp, x = arcRadius)
        ) {
            Text(
                text = sunsetText,
                color = sunsetTextColor,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = sunsetTime.getFormattedDate(timeFormat),
                color = sunsetTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}