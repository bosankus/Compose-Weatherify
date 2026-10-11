package bose.ankush.network.api

import bose.ankush.network.model.Account
import bose.ankush.network.model.AccountPhoto
import bose.ankush.network.model.ApiResponse
import bose.ankush.network.model.DeleteAccountResult

/** Account calls. The JWT is attached by the authorized-request helper. No account id is sent. */
interface AccountApiService {
    /** GET /account */
    suspend fun getAccount(): ApiResponse<Account>

    /**
     * POST /account/photo, multipart field `file`. The server accepts JPEG, PNG, and WebP up to
     * 5 MB and rejects HEIC and SVG. The response carries the new signed URL.
     */
    suspend fun uploadAccountPhoto(
        bytes: ByteArray,
        contentType: String,
        fileName: String,
    ): ApiResponse<AccountPhoto>

    /** DELETE /account/photo */
    suspend fun deleteAccountPhoto(): ApiResponse<AccountPhoto>

    /**
     * DELETE /account with a JSON body carrying the current password. Classified by HTTP status
     * rather than by parsing the envelope, because a 429 may not carry one.
     */
    suspend fun deleteAccount(password: String): DeleteAccountResult
}
