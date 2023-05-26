package bose.ankush.weatherify.presentation.air_quality

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.component.ScreenTopAppBar
import bose.ankush.weatherify.presentation.navigation.Screen

@Composable
internal fun AirQualityDetailsScreen(
    lat: Double?,
    lon: Double?,
    navController: NavController,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                ScreenTopAppBar(
                    headlineId = R.string.air_quality,
                    navIconAction = { navController.popBackStack() }
                )
            },
            content = { innerPadding ->
                Column(
                    modifier = Modifier.padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "lat = $lat, lon = $lon",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable {
                            navController.navigate(
                                Screen.HomeScreen.withArgs(
                                    "Adra"
                                )
                            )
                        }
                    )
                }
            }
        )
    }
}