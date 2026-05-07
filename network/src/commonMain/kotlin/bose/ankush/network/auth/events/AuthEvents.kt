package bose.ankush.network.auth.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Global authentication-related events emitted from the network layer.
 * The app layer can observe these to react (e.g., navigate to Login on 401).
 */
sealed class AuthEvent {
    data class Unauthorized(
        val message: String,
    ) : AuthEvent()
}

object AuthEventBus {
    private val _events =
        MutableSharedFlow<AuthEvent>(
            replay = 1,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<AuthEvent> = _events

    /** Suspends only if buffer == capacity after dropping oldest. */
    suspend fun emit(event: AuthEvent) = _events.emit(event)

    /** Never suspends; drops oldest if full. */
    fun tryEmit(event: AuthEvent): Boolean = _events.tryEmit(event)
}
