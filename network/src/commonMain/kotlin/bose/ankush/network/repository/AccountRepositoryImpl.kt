package bose.ankush.network.repository

import bose.ankush.network.api.AccountApiService
import bose.ankush.network.model.Account

class AccountRepositoryImpl(
    private val apiService: AccountApiService,
) : AccountRepository {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun getAccount(): Result<Account> =
        try {
            val response = apiService.getAccount()
            val account = response.data
            if (response.status && account != null) {
                Result.success(account)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
