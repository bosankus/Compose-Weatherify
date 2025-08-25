package bose.ankush.network.auth.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Global authentication-related events emitted from the network layer.
 * The app layer can observe these to react (e.g., navigate to Login on 401).
 */
sealed class AuthEvent {
    data class Unauthorized(val message: String) : AuthEvent()
}

object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events

    suspend fun emit(event: AuthEvent) {
        _events.emit(event)
    }
}
