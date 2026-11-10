package bose.ankush.auth.presentation

sealed interface AuthIntent {
    data class Login(
        val email: String,
        val password: String,
    ) : AuthIntent

    data class Register(
        val email: String,
        val password: String,
    ) : AuthIntent

    object Logout : AuthIntent

    object Reset : AuthIntent

    /** Refreshes now, e.g. after a purchase, to pick up the new premium status. */
    object RefreshToken : AuthIntent

    /** The app came to the foreground: refreshes unless it did so within the last few minutes. */
    object RefreshOnForeground : AuthIntent
}
