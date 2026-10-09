package bose.ankush.home.domain.usecase

import bose.ankush.network.repository.AccountRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The account photo as every screen sees it. Settings changes it through the same repository,
 * so the home avatar follows without a refetch. Null shows the default avatar.
 */
internal class ObserveAccountPhotoUrl(
    private val repository: AccountRepository,
) {
    operator fun invoke(): Flow<String?> =
        repository.account.map { it?.photoUrl }.distinctUntilChanged()
}

/**
 * GET /account. The signed photo URL is short-lived, so the home screen asks again each time
 * it opens. The result reaches [ObserveAccountPhotoUrl]; a failure leaves the last photo.
 */
internal class RefreshAccount(
    private val repository: AccountRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(): Result<Unit> =
        withContext(ioDispatcher) { repository.getAccount().map { } }
}
