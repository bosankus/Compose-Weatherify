package bose.ankush.network.api

import bose.ankush.network.model.Account
import bose.ankush.network.model.AccountPhoto
import bose.ankush.network.model.ApiResponse

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
}
