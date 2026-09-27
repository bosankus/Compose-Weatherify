package bose.ankush.analytics

import platform.Foundation.NSLog
import kotlin.coroutines.cancellation.CancellationException

/**
 * Placeholder until Crashlytics is available on iOS.
 *
 * Wiring it up needs two things this module can't do on its own: a `FirebaseCrashlytics` product
 * in the swiftPMDependencies block, and a dSYM upload build phase in the Xcode project so reports
 * symbolicate. Until then errors go to the device console so they are at least visible in a debug
 * session, and every call site is already reporting — swapping this implementation is the only
 * change needed.
 */
internal class NoOpErrorReporter : ErrorReporter {
    private var enabled = true
    private val keys = mutableMapOf<String, String>()

    override fun recordError(
        throwable: Throwable,
        message: String?,
        context: Map<String, String>,
    ) {
        if (!enabled) return
        if (throwable is CancellationException) return
        val line =
            (message ?: throwable.message.orEmpty()) +
                if (context.isEmpty()) "" else context.entries.joinToString(" ", " [", "]") { "${it.key}=${it.value}" }
        NSLog("[ErrorReporter] $line :: $throwable")
    }

    override fun log(message: String) {
        if (!enabled) return
        NSLog("[ErrorReporter] $message")
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun setCustomKey(
        key: String,
        value: String,
    ) {
        keys[key] = value
    }
}
