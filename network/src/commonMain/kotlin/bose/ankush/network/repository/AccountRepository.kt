package bose.ankush.network.repository

import bose.ankush.network.model.Account

interface AccountRepository {
    suspend fun getAccount(): Result<Account>

    /** Signed photo URL from GET /account/photo, or null when the user has no photo. */
    suspend fun getAccountPhotoUrl(): Result<String?>
}
