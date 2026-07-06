package bose.ankush.home.presentation.home.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.components.ShimmerEffect

@Composable
internal fun ShowLoading(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CurrentWeatherSkeleton()
        HourlyForecastSkeleton()
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
            ShimmerEffect(
                height = 16.dp,
                cornerRadius = 4.dp,
                modifier = Modifier.fillMaxWidth(0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.size(64.dp)) {
                ShimmerEffect(height = 64.dp, cornerRadius = 32.dp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            ShimmerEffect(
                height = 40.dp,
                cornerRadius = 8.dp,
                modifier = Modifier.fillMaxWidth(0.4f),
            )
            Spacer(modifier = Modifier.height(20.dp))
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
private fun HourlyForecastSkeleton() {
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ShimmerEffect(
                            height = 10.dp,
                            cornerRadius = 4.dp,
                            modifier = Modifier.width(28.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.size(32.dp)) {
                            ShimmerEffect(height = 32.dp, cornerRadius = 16.dp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerEffect(
                            height = 12.dp,
                            cornerRadius = 4.dp,
                            modifier = Modifier.width(24.dp),
                        )
                    }
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
            repeat(3) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ShimmerEffect(height = 14.dp, cornerRadius = 4.dp, modifier = Modifier.fillMaxWidth(0.2f))
                    ShimmerEffect(height = 24.dp, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth(0.15f))
                    ShimmerEffect(height = 14.dp, cornerRadius = 4.dp, modifier = Modifier.fillMaxWidth(0.15f))
                }
                if (index != 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
