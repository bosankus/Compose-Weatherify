package bose.ankush.auth.presentation

sealed interface AuthState {
    data object Initial : AuthState

    data object Loading : AuthState

    data object LogoutLoading : AuthState

    data object Success : AuthState

    data object LoggedOut : AuthState

    data class Error(
        val message: String,
    ) : AuthState

    data class SessionExpired(
        val message: String,
    ) : AuthState
}
