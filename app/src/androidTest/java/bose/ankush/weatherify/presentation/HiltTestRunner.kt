package bose.ankush.weatherify.presentation

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

@SuppressWarnings("Unused symbol")
class HiltTestRunner: AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication_Application::class.java.name, context)
    }
}