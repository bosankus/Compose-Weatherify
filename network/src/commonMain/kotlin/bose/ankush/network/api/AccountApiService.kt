package bose.ankush.network.api

import bose.ankush.network.model.Account
import bose.ankush.network.model.AccountPhoto
import bose.ankush.network.model.ApiResponse

/** Account reads. The JWT is attached by the authorized-request helper. No account id is sent. */
interface AccountApiService {
    /** GET /account */
    suspend fun getAccount(): ApiResponse<Account>

    /** GET /account/photo */
    suspend fun getAccountPhoto(): ApiResponse<AccountPhoto>
}
