package bose.ankush.home.presentation.screen

import bose.ankush.network.model.UnsplashPhoto

data class BackgroundPhoto(
    val id: String,
    val imageUrl: String,
    val photographer: String,
    val profileUrl: String,
    val downloadLocation: String,
)

/** OpenWeather `weather.main` to an Unsplash search. Unknown mains use one default. */
internal fun unsplashQuery(weatherMain: String): String =
    when (weatherMain.lowercase()) {
        "clear" -> "clear sky landscape"
        "clouds" -> "clouds landscape"
        "rain", "drizzle" -> "rain landscape"
        "snow" -> "snow landscape"
        "thunderstorm", "squall", "tornado" -> "thunderstorm landscape"
        "mist" -> "mist landscape"
        "fog" -> "fog landscape"
        else -> "weather landscape"
    }

internal fun UnsplashPhoto.toBackgroundPhoto(): BackgroundPhoto =
    BackgroundPhoto(
        id = id,
        imageUrl = unsplashBackgroundUrl(urls.raw),
        photographer = user.name,
        profileUrl = unsplashReferralUrl(user.links.html),
        downloadLocation = links.downloadLocation,
    )

internal fun unsplashBackgroundUrl(raw: String): String {
    val joiner = if ('?' in raw) "&" else "?"
    return raw + joiner + "w=1080&h=1920&fit=crop&fm=jpg&dpr=2"
}

internal fun unsplashReferralUrl(profileHtml: String): String {
    val joiner = if ('?' in profileHtml) "&" else "?"
    return profileHtml + joiner + "utm_source=weatherify&utm_medium=referral"
}

internal const val UNSPLASH_HOME_URL =
    "https://unsplash.com/?utm_source=weatherify&utm_medium=referral"
