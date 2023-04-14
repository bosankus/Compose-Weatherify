package bose.ankush.weatherify.base.language.languagetoggle

import android.content.Context

interface LanguageToggleManager {

    fun createLanguageList(): List<String>

    fun onLanguageChangeClicked(index: Int, context: Context)
}