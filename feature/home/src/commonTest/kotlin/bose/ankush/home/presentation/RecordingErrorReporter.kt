package bose.ankush.home.presentation

import bose.ankush.analytics.ErrorReporter

/** Keeps every [recordError] call so tests can check what was reported. */
internal class RecordingErrorReporter : ErrorReporter {
    val recorded = mutableListOf<Pair<Throwable, Map<String, String>>>()

    override fun recordError(
        throwable: Throwable,
        message: String?,
        context: Map<String, String>,
    ) {
        recorded += throwable to context
    }

    override fun log(message: String) = Unit

    override fun setEnabled(enabled: Boolean) = Unit

    override fun setCustomKey(
        key: String,
        value: String,
    ) = Unit
}
