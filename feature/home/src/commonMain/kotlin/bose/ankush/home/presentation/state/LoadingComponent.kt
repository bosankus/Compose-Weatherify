package bose.ankush.home.presentation.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.components.ShimmerEffect

@Composable
internal fun ShowLoading(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CurrentWeatherSkeleton()
        DailyForecastSkeleton()
    }
}

@Composable
private fun CurrentWeatherSkeleton() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    ShimmerEffect(
                        height = 16.dp,
                        cornerRadius = 4.dp,
                        modifier = Modifier.fillMaxWidth(0.5f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ShimmerEffect(
                        height = 40.dp,
                        cornerRadius = 8.dp,
                        modifier = Modifier.fillMaxWidth(0.4f),
                    )
                }
                Box(modifier = Modifier.size(64.dp)) {
                    ShimmerEffect(height = 64.dp, cornerRadius = 32.dp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(3) {
                    ShimmerEffect(
                        height = 14.dp,
                        cornerRadius = 4.dp,
                        modifier = Modifier.fillMaxWidth(0.25f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastSkeleton() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerEffect(
                height = 16.dp,
                cornerRadius = 4.dp,
                modifier = Modifier.fillMaxWidth(0.35f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ShimmerEffect(
                    height = 14.dp,
                    cornerRadius = 4.dp,
                    modifier = Modifier.fillMaxWidth(0.8f),
                )
                ShimmerEffect(
                    height = 14.dp,
                    cornerRadius = 4.dp,
                    modifier = Modifier.fillMaxWidth(0.8f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ShowLoadingPreview() {
    MaterialTheme {
        Surface {
            ShowLoading(modifier = Modifier)
        }
    }
}

@Preview
@Composable
private fun CurrentWeatherSkeletonPreview() {
    MaterialTheme {
        Surface {
            CurrentWeatherSkeleton()
        }
    }
}

@Preview
@Composable
private fun DailyForecastSkeletonPreview() {
    MaterialTheme {
        Surface {
            DailyForecastSkeleton()
        }
    }
}
