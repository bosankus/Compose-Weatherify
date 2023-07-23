package bose.ankush.weatherify.presentation.home.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.presentation.air_quality.AirQualityIndexAnalyser.getAQIAnalysedText
import bose.ankush.weatherify.presentation.air_quality.AirQualityIndexAnalyser.getFormattedAQI
import bose.ankush.weatherify.presentation.home.HomeViewModel

/**
 * This composable is response to show air quality card on HomeScreen.
 * Shows what is the current air quality based return value of [getAQIAnalysedText]
 */
@SuppressLint("MissingPermission")
@Composable
internal fun AirQualityCardLayout(
    viewModel: HomeViewModel, onCardClick: () -> Unit
) {
    val airQualityReport = remember { viewModel.airQuality.value }

    ShowUI(
        aq = airQualityReport.data ?: AirQuality(), onItemClick = onCardClick
    )
}

/**
 * Air quality UI composable
 * This composable has onClick listener, with action to navigate to AirQualityDetailsScreen,
 * and carry latitude and longitude as navigation arguments
 */
@Composable
private fun ShowUI(
    aq: AirQuality, onItemClick: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(all = 16.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp))
        .padding(horizontal = 15.dp, vertical = 20.dp)) {
        Text(
            modifier = Modifier.alpha(0.6f),
            text = aq.aqi?.getFormattedAQI() ?: "00",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.alpha(0.6f),
            text = "Air Quality Index",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.padding(top = 5.dp),
            text = aq.aqi?.let { getAQIAnalysedText(it) }?.first ?: "Something went wrong",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            modifier = Modifier
                .clickable { onItemClick() }
                .padding(top = 10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                text = "See more",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}