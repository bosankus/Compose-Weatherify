package bose.ankush.weatherify.base.language.languagetoggle

import android.content.Context
import bose.ankush.weatherify.base.language.LocaleService
import bose.ankush.weatherify.domain.model.Country
import bose.ankush.weatherify.preferences.UserPreferences

class LanguageToggleManagerImpl(
    val country: Country,
    val localeService: LocaleService,
    val userPreferences: UserPreferences
): LanguageToggleManager  {

    override fun createLanguageList(): List<String> {
        TODO("Not yet implemented")
    }

    override fun onLanguageChangeClicked(index: Int, context: Context) {
        TODO("Not yet implemented")
    }


}