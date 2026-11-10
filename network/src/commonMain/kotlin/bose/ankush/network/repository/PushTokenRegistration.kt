package bose.ankush.network.repository

import bose.ankush.network.api.AccountApiService
import kotlinx.coroutines.CancellationException

/** This device's push token, from the platform's messaging SDK. Null when it has none. */
fun interface PushTokenProvider {
    suspend fun token(): String?
}

/**
 * Keeps the server's list of this account's devices current, so a change made on one device
 * (a new profile photo) reaches the others as a silent push. Every call is best effort: a
 * device that misses a push still sees the change on its next GET /account.
 */
class PushTokenRegistration(
    private val api: AccountApiService,
    private val provider: PushTokenProvider,
) {
    /** After sign-in, on every launch while signed in, and when the SDK rotates the token. */
    suspend fun register(token: String? = null) {
        val current = token ?: provider.token() ?: return
        quietly { api.registerPushToken(current) }
    }

    /** Before logout, while the session can still authenticate the call. */
    suspend fun unregister() {
        val current = provider.token() ?: return
        quietly { api.unregisterPushToken(current) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun quietly(call: suspend () -> Unit) {
        try {
            call()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best effort, see the class doc.
        }
    }
}

/** The `type` of the silent push sent after the account's photo changed. */
const val ACCOUNT_PHOTO_CHANGED_PUSH = "account_photo_changed"
