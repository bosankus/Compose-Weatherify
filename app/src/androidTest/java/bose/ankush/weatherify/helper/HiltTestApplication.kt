package bose.ankush.weatherify.helper

import bose.ankush.weatherify.WeatherifyApplicationCore
import dagger.hilt.android.testing.CustomTestApplication

@CustomTestApplication(WeatherifyApplicationCore::class)
interface HiltTestApplication