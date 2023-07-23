package bose.ankush.weatherify.presentation

import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.hilt.navigation.compose.hiltViewModel
import bose.ankush.weatherify.presentation.navigation.AppNavigation
import bose.ankush.weatherify.presentation.theme.WeatherifyTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalAnimationApi::class, ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class MainActivityTest {

    @get: Rule(order = 1)
    val hiltAndroidRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val createComposeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        hiltAndroidRule.inject()
    }

    @Test
    fun verify_HomeScreen_isShown() {
        createComposeRule.activity.setContent {
            viewModel = hiltViewModel()
            WeatherifyTheme { AppNavigation() }
        }
    }
}
