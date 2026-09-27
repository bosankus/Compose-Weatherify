package bose.ankush.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.coroutines.cancellation.CancellationException

/**
 * The only class in the app that touches the Crashlytics SDK. Everything else — shared code via
 * [ErrorReporter], Android code via Timber and `CrashlyticsTree` — funnels through this one
 * instance, resolved once here rather than per call.
 */
internal class CrashlyticsErrorReporter : ErrorReporter {
    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun recordError(
        throwable: Throwable,
        message: String?,
        context: Map<String, String>,
    ) {
        // Cancellation is normal control flow (screen left, collectLatest restarted). Reporting it
        // would bury real failures under noise.
        if (throwable is CancellationException) return

        // Per-report context goes into the log line, NOT setCustomKey. Custom keys live for the
        // whole session, so writing per-error values there would leave the last failure's endpoint
        // attached to every later report — including an unrelated fatal crash. setCustomKey is
        // reserved for state that really is session-wide (build type, version, sign-in state).
        crashlytics.log(buildLogLine(message ?: throwable.message.orEmpty(), context))
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setEnabled(enabled: Boolean) {
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }

    override fun setCustomKey(
        key: String,
        value: String,
    ) {
        crashlytics.setCustomKey(key, value)
    }
}

internal fun buildLogLine(
    message: String,
    context: Map<String, String>,
): String =
    if (context.isEmpty()) {
        message
    } else {
        context.entries.joinToString(
            separator = " ",
            prefix = "$message [",
            postfix = "]",
        ) { "${it.key}=${it.value}" }
    }
