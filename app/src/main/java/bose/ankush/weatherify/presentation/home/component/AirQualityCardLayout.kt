package bose.ankush.weatherify.presentation.home.component

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.Extension.openAppSystemSettings
import bose.ankush.weatherify.common.LocationPermissionManager
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.presentation.air_quality.AirQualityIndexAnalyser.getAQIAnalysedText
import bose.ankush.weatherify.presentation.home.HomeViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.Priority
import timber.log.Timber

/**
 * This composable is response to show air quality card on HomeScreen.
 * Shows what is the current air quality based return value of [getAQIAnalysedText]
 */
@SuppressLint("MissingPermission")
@ExperimentalPermissionsApi
@Composable
internal fun AirQualityCardLayout(
    viewModel: HomeViewModel = hiltViewModel(),
    onCardClick: (Double, Double) -> Unit
) {

    val context: Context = LocalContext.current
    val airQualityReport = remember { viewModel.airQuality.value }
    val latLang = remember { mutableStateOf(Pair(0.0, 0.0)) }

    /**
     * Checking/requesting permission
     * If granted fusedLocationProvider will get current location of the user,
     * If denied, next time request will go to app settings screen.
     */
    LocationPermissionManager.RequestPermission(
        actionPermissionGranted = {
            viewModel.fusedLocationProviderClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )
                .addOnSuccessListener { location ->
                    if (location != null) {
                        latLang.value = latLang.value.copy(first = location.latitude)
                        latLang.value = latLang.value.copy(second = location.longitude)
                        Timber.d("Androidplay: Fetched coordinates = ${latLang.value.first}, ${latLang.value.second}")
                    }
                }
        },
        actionPermissionDenied = { context.openAppSystemSettings() }
    )

    /**
     * If location details are fetched, then only make data request
     * 0.0 is default value for both latitude and longitude.
     */
    if (latLang.value.first != 0.0 && latLang.value.second != 0.0)
        LaunchedEffect(Unit) {
            viewModel.fetchAirQuality(latLang.value.first, latLang.value.second)
        }

    // if air quality report is not empty, show UI
    AnimatedVisibility(
        visible = airQualityReport.data != null
    ) {
        ShowUI(
            aq = airQualityReport.data ?: AirQuality(),
            onItemClick = onCardClick
        )
    }
}

/**
 * Air quality UI composable
 * This composable has onClick listener, with action to navigate to AirQualityDetailsScreen,
 * and carry latitude and longitude as navigation arguments
 */
@Composable
private fun ShowUI(
    aq: AirQuality,
    onItemClick: (Double, Double) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
            .clickable { onItemClick(aq.coord.first, aq.coord.second) }
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 15.dp, vertical = 20.dp)
    ) {
        Text(
            text = aq.aqi?.let { getAQIAnalysedText(it) }?.first ?: "Something went wrong",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.inversePrimary,
            overflow = TextOverflow.Ellipsis,
        )
        AsyncImage(
            modifier = Modifier.size(16.dp),
            model = R.drawable.ic_chevron_right,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.inversePrimary),
            contentDescription = stringResource(id = R.string.weather_icon_content),
        )
    }
}