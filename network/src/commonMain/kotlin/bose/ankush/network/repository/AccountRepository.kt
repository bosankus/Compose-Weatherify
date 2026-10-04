package bose.ankush.network.repository

import bose.ankush.network.model.Account

interface AccountRepository {
    suspend fun getAccount(): Result<Account>
}
