package bose.ankush.auth.presentation

sealed interface AuthState {
    object Initial : AuthState

    object Loading : AuthState

    object LogoutLoading : AuthState

    object Success : AuthState

    object LoggedOut : AuthState

    data class Error(
        val message: String,
    ) : AuthState

    data class SessionExpired(
        val message: String,
    ) : AuthState
}
