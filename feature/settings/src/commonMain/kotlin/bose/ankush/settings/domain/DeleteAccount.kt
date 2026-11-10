package bose.ankush.settings.domain

import bose.ankush.network.auth.events.AuthEvent
import bose.ankush.network.auth.events.AuthEventBus
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.DeleteAccountResult
import bose.ankush.network.repository.AccountRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * DELETE /account. Only a confirmed deletion ends the session: every other outcome, including a
 * server error after which the account may still exist, leaves the token and local data alone.
 */
internal class DeleteAccount(
    private val repository: AccountRepository,
    private val endSession: suspend () -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    constructor(repository: AccountRepository, tokenManager: TokenManager) :
            this(repository, { endDeletedSession(tokenManager) })

    suspend operator fun invoke(password: String): DeleteAccountResult {
        val result = withContext(ioDispatcher) { repository.deleteAccount(password) }
        if (result == DeleteAccountResult.Deleted) endSession()
        return result
    }
}

/**
 * Announces the deletion before dropping the token, so the app's cleanup and its "account deleted"
 * message are already underway when clearing the token swaps the screen to sign-in.
 */
private suspend fun endDeletedSession(tokenManager: TokenManager) {
    AuthEventBus.emit(AuthEvent.AccountDeleted)
    tokenManager.forceLogout()
}
