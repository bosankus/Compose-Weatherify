package bose.ankush.weatherify.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import bose.ankush.weatherify.navigation.AppNavigation
import bose.ankush.weatherify.presentation.ui.theme.WeatherifyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherifyTheme {
                AppNavigation()
            }
        }
    }

}