package bose.ankush.weatherify.presentation

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import bose.ankush.weatherify.R
import org.junit.Before
import org.junit.Test

class MainActivityTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.RESUMED)
    }


    @Test
    fun test_ui_flow_when_activity_isShown() {
        // Check initial loading
        onView(withId(R.id.activity_main_layout_loading)).check(matches(isDisplayed()))

        // Wait to let the data load
        onView(ViewMatchers.isRoot()).perform(waitFor(1000))

        // Check text views showing correct data
        onView(withId(R.id.activity_main_layout_loading)).check(
            matches(
                ViewMatchers.withEffectiveVisibility(
                    ViewMatchers.Visibility.GONE
                )
            )
        )
        onView(withId(R.id.layout_weather_current_temp)).check(
            matches(
                ViewMatchers.withText(
                    "30°C"
                )
            )
        )
        onView(withId(R.id.layout_weather_current_city)).check(
            matches(
                ViewMatchers.withText(
                    "Kolkata"
                )
            )
        )

        // Wait to let the recyclerview animate and load data
        onView(ViewMatchers.isRoot()).perform(waitFor(1000))

        // Check Recyclerview is showing
        onView(withId(R.id.layout_weather_recyclerview)).check(matches(isDisplayed()))
    }
}