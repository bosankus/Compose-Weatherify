package bose.ankush.weatherify.presentation.splash

import android.annotation.SuppressLint
import android.content.Context
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.common.DEFAULT_LOCATION_COORDINATES
import bose.ankush.weatherify.common.Extension.openAppSystemSettings
import bose.ankush.weatherify.common.LocationPermissionManager
import bose.ankush.weatherify.presentation.home.HomeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.delay


@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SplashScreen(
    viewModel: HomeViewModel,
    navAction: () -> Unit
) {
    val context: Context = LocalContext.current
    val latLang = remember { mutableStateOf(DEFAULT_LOCATION_COORDINATES) }
    val appIconScale = remember { Animatable(0.0f) }

    // Handle current location fetching
    LocationPermissionManager.RequestPermission(
        // when permission is granted
        actionPermissionGranted = {
            viewModel.fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        latLang.value = latLang.value.copy(first = location.latitude)
                        latLang.value = latLang.value.copy(second = location.longitude)
                        // stores user's current location in datastore
                        viewModel.saveUserLocationPreference(
                            Pair(
                                location.latitude,
                                location.longitude
                            )
                        )
                    }
                }
        },
        /**
         * when permission is not granted, and dialog forces user to open settings to enable
         * location access
         */
        actionPermissionDenied = { context.openAppSystemSettings() }
    )

    // Splash screen app icon animation
    LaunchedEffect(key1 = true) {
        appIconScale.animateTo(
            targetValue = 0.7f,
            animationSpec = tween(
                durationMillis = 800,
                easing = { OvershootInterpolator(4f).getInterpolation(it) }
            )
        )
        delay(800)

        // Go to home screen
        navAction.invoke()
    }



    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .size(120.dp)
                .scale(appIconScale.value),
            imageVector = Icons.Filled.Person,
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = null
        )
    }
}
