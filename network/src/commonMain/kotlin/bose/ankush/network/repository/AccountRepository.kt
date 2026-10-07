package bose.ankush.network.repository

import bose.ankush.network.model.Account
import kotlinx.coroutines.flow.StateFlow

/**
 * The signed-in account. [account] is the single in-memory copy every screen observes, so a
 * photo changed on one screen shows on the others without a refetch. Photo URLs are signed and
 * short-lived, so nothing here is persisted.
 */
interface AccountRepository {
    /** Last account the server returned this session, or null before the first fetch. */
    val account: StateFlow<Account?>

    /** GET /account, then publishes it on [account]. */
    suspend fun getAccount(): Result<Account>

    /** POST /account/photo. Returns the new signed URL and publishes it on [account]. */
    suspend fun uploadPhoto(
        bytes: ByteArray,
        contentType: String,
        fileName: String,
    ): Result<String?>

    /** DELETE /account/photo, then clears the photo on [account]. */
    suspend fun deletePhoto(): Result<Unit>

    /** Forgets the cached account, on logout. */
    fun clear()
}
