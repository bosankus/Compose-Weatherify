package bose.ankush.network.api

import bose.ankush.network.model.Account
import bose.ankush.network.model.ApiResponse

/** GET /account. The JWT is attached by the authorized-request helper. No account id is sent. */
interface AccountApiService {
    suspend fun getAccount(): ApiResponse<Account>
}
