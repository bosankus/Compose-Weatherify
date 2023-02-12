package bose.ankush.weatherify.presentation.home.component

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.common.Extension.openAppSystemSettings
import bose.ankush.weatherify.common.LocationPermissionManager
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.presentation.analyzer.AirQualityIndexAnalyser.getAQIAnalysedText
import bose.ankush.weatherify.presentation.home.HomeViewModel
import bose.ankush.weatherify.presentation.ui.theme.DefaultCardBackgroundLightGrey
import bose.ankush.weatherify.presentation.ui.theme.TextWhite
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.Priority
import timber.log.Timber

@SuppressLint("MissingPermission")
@ExperimentalPermissionsApi
@Composable
fun AirQualityLayout(viewModel: HomeViewModel) {

    val airQualityReport = viewModel.airQuality.value
    val context: Context = LocalContext.current
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

    // if air quality report is not empty, UI will show
    if (airQualityReport.data != null)
        ShowUI(aq = airQualityReport.data)
}

// Air quality UI composable
@Composable
fun ShowUI(aq: AirQuality) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(all = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DefaultCardBackgroundLightGrey)
            .padding(horizontal = 15.dp, vertical = 20.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = aq.aqi?.let { getAQIAnalysedText(it) }?.first ?: "Something went wrong",
            style = MaterialTheme.typography.body1,
            color = TextWhite,
            overflow = TextOverflow.Ellipsis,
        )

        Image(
            painter = rememberAsyncImagePainter(model = "https://img.icons8.com/windows/26/f45164/wind.svg"),
            contentDescription = null,
            modifier = Modifier.size(26.dp)
        )

    }
}
