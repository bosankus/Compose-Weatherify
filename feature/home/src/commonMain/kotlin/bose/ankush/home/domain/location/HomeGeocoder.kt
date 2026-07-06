package bose.ankush.home.domain.location

internal interface HomeGeocoder {
    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): String?
}
