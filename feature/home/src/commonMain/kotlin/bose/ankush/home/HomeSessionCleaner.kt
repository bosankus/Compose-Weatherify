package bose.ankush.home

/** Called by the host app's logout flow to clear Home's locally cached weather data + location prefs. */
interface HomeSessionCleaner {
    suspend fun clearOnLogout()
}
