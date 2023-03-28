package bose.ankush.weatherify.domain.model

/**
 * Country contains basic information about a specific country
 * @property code: name of country in lower case
 * @property codeA2: contains ISO 3166-2 code
 * @property defaultLanguage: string which matches with device default language code (string resource)
 * @property localCurrency: string in upper case which matches with country's available currency code
 */
data class Country(
    val code: String,
    val codeA2: String,
    val defaultLanguage: String?,
    val languages: List<String>,
    val localCurrency: List<String>
)