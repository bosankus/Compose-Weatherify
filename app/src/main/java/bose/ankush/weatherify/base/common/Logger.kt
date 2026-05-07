package bose.ankush.weatherify.base.common

/**
 * Platform-agnostic logging interface.
 * Replaces direct Timber usage in shared/common code to enable KMP compatibility.
 */
interface Logger {
    fun d(message: String)

    fun i(message: String)

    fun w(message: String)

    fun e(
        message: String,
        throwable: Throwable? = null,
    )

    fun v(message: String)
}

/**
 * Factory that creates [Logger] instances scoped to a specific tag.
 * Android provides a Timber-backed implementation; other platforms can provide their own.
 */
interface LoggerFactory {
    fun create(tag: String): Logger
}
