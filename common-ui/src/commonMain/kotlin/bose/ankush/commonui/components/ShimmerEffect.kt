package bose.ankush.commonui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    cornerRadius: Dp = 4.dp,
    baseColor: Color = Color.LightGray.copy(alpha = 0.3f),
    highlightColor: Color = Color.White.copy(alpha = 0.8f),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX =
        infiniteTransition.animateFloat(
            initialValue = -1000f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        androidx.compose.animation.core.tween(
                            durationMillis = 1200,
                            easing = LinearEasing,
                        ),
                ),
            label = "shimmer_x",
        )

    val shimmerBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    baseColor,
                    highlightColor,
                    baseColor,
                ),
            start = Offset(shimmerX.value - 200f, 0f),
            end = Offset(shimmerX.value + 200f, 0f),
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    brush = shimmerBrush,
                    shape = RoundedCornerShape(cornerRadius),
                ),
    )
}

@Composable
fun ShimmerBottomSheetSkeleton(
    modifier: Modifier = Modifier,
    baseColor: Color = Color.LightGray.copy(alpha = 0.3f),
    highlightColor: Color = Color.White.copy(alpha = 0.8f),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        ShimmerEffect(
            height = 28.dp,
            cornerRadius = 6.dp,
            baseColor = baseColor,
            highlightColor = highlightColor,
            modifier =
                Modifier
                    .fillMaxWidth(0.6f)
                    .padding(bottom = 16.dp),
        )

        repeat(2) {
            ShimmerEffect(
                height = 14.dp,
                cornerRadius = 4.dp,
                baseColor = baseColor,
                highlightColor = highlightColor,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
            )
        }

        ShimmerEffect(
            height = 14.dp,
            cornerRadius = 4.dp,
            baseColor = baseColor,
            highlightColor = highlightColor,
            modifier =
                Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = 16.dp),
        )

        ShimmerEffect(
            height = 18.dp,
            cornerRadius = 4.dp,
            baseColor = baseColor,
            highlightColor = highlightColor,
            modifier =
                Modifier
                    .fillMaxWidth(0.3f)
                    .padding(bottom = 12.dp),
        )

        repeat(3) {
            ShimmerEffect(
                height = 14.dp,
                cornerRadius = 4.dp,
                baseColor = baseColor,
                highlightColor = highlightColor,
                modifier =
                    Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 10.dp),
            )
        }

        ShimmerEffect(
            height = 20.dp,
            cornerRadius = 6.dp,
            baseColor = baseColor,
            highlightColor = highlightColor,
            modifier =
                Modifier
                    .fillMaxWidth(0.4f)
                    .padding(top = 16.dp, bottom = 12.dp),
        )

        ShimmerEffect(
            height = 14.dp,
            cornerRadius = 4.dp,
            baseColor = baseColor,
            highlightColor = highlightColor,
            modifier =
                Modifier
                    .fillMaxWidth(0.5f)
                    .padding(bottom = 20.dp),
        )

        ShimmerEffect(
            height = 48.dp,
            cornerRadius = 8.dp,
            baseColor = baseColor,
            highlightColor = highlightColor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
