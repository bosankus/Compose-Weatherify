package bose.ankush.weatherify.helper

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application =
        super.newApplication(cl, HiltTestApplication_Application::class.java.name, context)
}