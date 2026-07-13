package bose.ankush.weatherify.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WeatherifyWearApp() }
    }
}

@Composable
private fun WeatherifyWearApp() {
    MaterialTheme {
        AppScaffold {
            ScreenScaffold {
                Text(
                    modifier = Modifier.fillMaxSize(),
                    text = "Hello Ankush",
                )
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun WeatherifyWearAppPreview() {
    WeatherifyWearApp()
}
