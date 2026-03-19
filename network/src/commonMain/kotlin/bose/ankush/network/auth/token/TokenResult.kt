package bose.ankush.network.auth.token

sealed class TokenResult {
    data class Valid(val token: String) : TokenResult()
    data object NoToken : TokenResult()
    data class InvalidToken(val errorCode: String?) : TokenResult()
    data class Error(val exception: Exception) : TokenResult()

    fun tokenOrNull(): String? = (this as? Valid)?.token
    fun isValid(): Boolean = this is Valid
}
