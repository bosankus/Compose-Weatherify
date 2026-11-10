package bose.ankush.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether Home has drawn its first real frame this process: the Room forecast (or the lack of
 * one) is known, so the screen shows numbers rather than placeholders. The host keeps the
 * system splash up until then, so the splash fades straight onto a finished screen.
 */
object HomeFirstContent {
    private val ready = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = ready.asStateFlow()

    internal fun markReady() {
        ready.value = true
    }
}
