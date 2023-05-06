package bose.ankush.weatherify.base

import android.content.Context
import bose.ankush.weatherify.domain.model.Country
import bose.ankush.weatherify.preferences.UserPreferences
import java.util.Locale

class LocaleService(
    private val context: Context,
    private val country: Country,
    private val  deviceService: DeviceService,
    private val userPreferences: UserPreferences
) {

    /**
     * This method returns device's locale object
     */
    fun getLocale(): Locale = getLocaleFromDevice()

    /**
     * This method saves new locale object settings as per current context
     * @param context: current context of application
     * @param locale: new locale object to be set
     * @return new context
     */
    fun setLocale(context: Context, locale: Locale): Context =
        createNewLocaleContext(context, locale)

    /**
     * This method returns current locale string of the device
     * for eg: "en_IN"
     */
    fun getLocaleStringFromDevice(): String {
        val country = getCountry()
        val deviceLanguage = deviceService.getDeviceLocale()?.language
        val languages =
            deviceLanguage?.let { lang -> country.languages.filter { it.startsWith(lang) } }
                .orEmpty()

        userPreferences.getLocaleString()?.let {
            if (it.isNotEmpty()) return it
        }

        if (languages.isNotEmpty()) return languages.first()

        country.defaultLanguage?.let {
            return it
        }

        return country.languages.first()
    }

    /**
     * This method returns country object
     */
    private fun getCountry(): Country = country

    /**
     * This method returns locale object as per [DeviceService]/stored [UserPreferences]
     */
    private fun getLocaleFromDevice(): Locale {
        val country = getCountry()
        val deviceLanguage = deviceService.getDeviceLocale()?.language
        val languages =
            deviceLanguage?.let { lang -> country.languages.filter { it.startsWith(lang) } }
                .orEmpty()

        userPreferences.getLocaleString()?.let {
            if (it.isNotEmpty()) return getLocaleFromString(it)
        }

        if (languages.isNotEmpty()) return getLocaleFromString(languages.first())

        country.defaultLanguage?.let {
            return getLocaleFromString(it)
        }

        return getLocaleFromString(country.languages.first())
    }

    /**
     * This method returns locale object on the basis of passed argument
     * @param localeString: language code for example "en_IN"
     */
    private fun getLocaleFromString(localeString: String?): Locale {
        val modifiedString = localeString?.split("_")
        val language = modifiedString?.get(0) ?: "en"
        val country = modifiedString?.get(1) ?: "IN"
        return Locale(language, country)
    }

    private fun createNewLocaleContext(oldContext: Context, locale: Locale): Context {
        return oldContext
    }
}