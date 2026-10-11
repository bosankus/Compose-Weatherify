package bose.ankush.auth.presentation

sealed interface AuthEffect {
    /** Emitted after successful login/register when the server reports a premium subscription. */
    data class PremiumStatusChanged(
        val isPremium: Boolean,
        val expiryMillis: Long?,
    ) : AuthEffect

    /** Emitted after a successful logout — callers should clear local weather data and prefs. */
    object LoggedOut : AuthEffect

    /**
     * Emitted once the server has deleted the account. Callers clear everything [LoggedOut] does,
     * plus anything else tied to the account (push token, checkout prefill).
     */
    object AccountDeleted : AuthEffect
}
