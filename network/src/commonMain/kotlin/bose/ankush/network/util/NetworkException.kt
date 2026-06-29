package bose.ankush.network.util

class NetworkException(
    val errorCode: Int,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val SERVER_ERROR = 500
        const val SERVICE_UNAVAILABLE = 503

        const val NETWORK_UNAVAILABLE = 1000
        const val TIMEOUT = 1001
        const val UNKNOWN_HOST = 1002
        const val UNKNOWN_ERROR = 1999

        fun fromException(e: Exception): NetworkException {
            val errorCodeRegex = Regex("(\\d{3})")
            val errorCodeMatch = errorCodeRegex.find(e.message ?: "")
            val errorCode = errorCodeMatch?.value?.toIntOrNull() ?: UNKNOWN_ERROR

            return NetworkException(
                errorCode = errorCode,
                message = e.message ?: "Unknown error",
                cause = e,
            )
        }
    }
}
