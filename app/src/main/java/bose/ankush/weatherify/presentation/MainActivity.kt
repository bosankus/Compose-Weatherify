package bose.ankush.weatherify.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import bose.ankush.weatherify.common.startInAppUpdate
import bose.ankush.weatherify.presentation.navigation.AppNavigation
import bose.ankush.weatherify.presentation.theme.WeatherifyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("getApplicationLocales: ${AppCompatDelegate.getApplicationLocales()}")
        setContent {
            WeatherifyTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AppNavigation()
                }
            }
        }

        startInAppUpdate(this)
    }

    override fun onResume() {
        super.onResume()
        startInAppUpdate(this)
    }
}