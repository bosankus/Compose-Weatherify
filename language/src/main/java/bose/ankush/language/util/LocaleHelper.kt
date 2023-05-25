package bose.ankush.language.util

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

internal object LocaleHelper {

    fun String.getDisplayName(): String {
        val locale = Locale(this)
        return locale.getDisplayName(locale)
    }

    fun changeLanguageTo(languageCode: String): LocaleListCompat {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2 || Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            // If Android 12
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageCode)
            )
        } else {
            // If Android 11 and below
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageCode)
            )
            // (context as AppCompatActivity).restartCurrentActivity()
        }
        return AppCompatDelegate.getApplicationLocales()
    }

    /*private fun AppCompatActivity.restartCurrentActivity() {
        val intent = Intent(this, this::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }*/
}