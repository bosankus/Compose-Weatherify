package bose.ankush.weatherify.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Country contains basic information about a specific country
 * @property code: name of country in lower case
 * @property codeA2: contains ISO 3166-2 code
 * @property defaultLanguage: string which matches with device default language code (string resource)
 * @property localCurrency: string in upper case which matches with country's available currency code
 */
@Parcelize
data class Country(
    val code: String = "india",
    val codeA2: String = "in",
    val defaultLanguage: String? = "en",
    val languages: List<String> = listOf("en"),
    val localCurrency: List<String> = listOf("INR")
): Parcelable