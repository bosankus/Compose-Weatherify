package bose.ankush.network.repository

import bose.ankush.network.api.AccountApiService
import bose.ankush.network.model.Account
import bose.ankush.network.model.ApiResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AccountRepositoryImpl(
    private val apiService: AccountApiService,
) : AccountRepository {
    private val _account = MutableStateFlow<Account?>(null)
    override val account: StateFlow<Account?> = _account.asStateFlow()

    override suspend fun getAccount(): Result<Account> =
        call { apiService.getAccount() }
            .mapCatching { it ?: error("Account is missing from the response") }
            .onSuccess { fetched ->
                _account.value = fetched.copy(photoUrl = fetched.photoUrl.nonBlank())
            }

    override suspend fun uploadPhoto(
        bytes: ByteArray,
        contentType: String,
        fileName: String,
    ): Result<String?> =
        call { apiService.uploadAccountPhoto(bytes, contentType, fileName) }
            .map { it?.photoUrl.nonBlank() }
            .onSuccess { url -> _account.update { (it ?: Account()).copy(photoUrl = url) } }

    override suspend fun deletePhoto(): Result<Unit> =
        call { apiService.deleteAccountPhoto() }
            .map { }
            .onSuccess { _account.update { it?.copy(photoUrl = null) } }

    override fun clear() {
        _account.value = null
    }

    /** Unwraps the envelope. Cancellation is rethrown, never reported as a failed call. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> call(request: suspend () -> ApiResponse<T>): Result<T?> =
        try {
            val response = request()
            if (response.status) Result.success(response.data) else Result.failure(
                Exception(
                    response.message
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }
}
