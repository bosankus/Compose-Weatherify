package bose.ankush.weatherify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

@HiltAndroidApp
class WeatherifyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}