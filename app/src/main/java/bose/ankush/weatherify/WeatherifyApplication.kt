package bose.ankush.weatherify

import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

@HiltAndroidApp
class WeatherifyApplication: WeatherifyApplicationCore() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}