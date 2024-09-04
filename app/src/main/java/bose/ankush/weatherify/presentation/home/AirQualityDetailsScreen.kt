package bose.ankush.weatherify.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.component.ScreenTopAppBar
import bose.ankush.weatherify.presentation.MainViewModel

@Composable
internal fun AirQualityDetailsScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val userLocation = remember { viewModel.userLocationPreference.value }
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
                    if (userLocation.data != null) {
                        Text(
                            text = "Your current location coordinate is: ${userLocation.data.first}, ${userLocation.data.second}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                }
            }
        )
    }
}