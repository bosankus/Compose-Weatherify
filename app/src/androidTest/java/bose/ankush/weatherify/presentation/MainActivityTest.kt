package bose.ankush.weatherify.presentation

import dagger.hilt.android.testing.HiltAndroidTest

@HiltAndroidTest
class MainActivityTest {

    /*@get: Rule(order = 1)
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
    }*/
}
