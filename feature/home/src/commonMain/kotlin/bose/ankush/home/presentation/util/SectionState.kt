package bose.ankush.home.presentation.util

/**
 * Load state of one independently fetched block of the screen. An empty result is a
 * [Loaded] value (an empty list, or null), not a separate state, so the UI decides how
 * "nothing" looks.
 */
internal sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>

    data object Failed : SectionState<Nothing>

    data class Loaded<out T>(
        val value: T,
    ) : SectionState<T>
}

internal fun <T> SectionState<T>.valueOrNull(): T? = (this as? SectionState.Loaded<T>)?.value
