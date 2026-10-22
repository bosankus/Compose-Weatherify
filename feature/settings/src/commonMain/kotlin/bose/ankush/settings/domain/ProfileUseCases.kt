package bose.ankush.settings.domain

import bose.ankush.network.model.Account
import bose.ankush.network.repository.AccountRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * A photo ready to upload. Platforms re-encode what the user picked to JPEG first: the server
 * rejects HEIC (the camera default on many phones) and anything over 5 MB.
 */
internal class ProfilePhoto(
    val bytes: ByteArray,
    val contentType: String = JPEG,
    val fileName: String = "profile.jpg",
) {
    companion object {
        const val JPEG = "image/jpeg"

        /** The server's limit. */
        const val MAX_BYTES = 5 * 1024 * 1024
    }
}

/** The signed-in account, shared with every screen that shows it. */
internal class ObserveAccount(
    private val repository: AccountRepository,
) {
    operator fun invoke(): Flow<Account?> = repository.account
}

/** GET /account. The photo URL is signed and short-lived, so the screen asks again on open. */
internal class RefreshAccount(
    private val repository: AccountRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(): Result<Unit> = withContext(ioDispatcher) { repository.getAccount().map { } }
}

/** POST /account/photo. A photo over the server's limit is refused before it is sent. */
internal class UploadProfilePhoto(
    private val repository: AccountRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(photo: ProfilePhoto): Result<Unit> {
        if (photo.bytes.isEmpty() || photo.bytes.size > ProfilePhoto.MAX_BYTES) {
            return Result.failure(IllegalArgumentException("Photo must be between 1 byte and 5 MB"))
        }
        return withContext(ioDispatcher) {
            repository.uploadPhoto(photo.bytes, photo.contentType, photo.fileName).map { }
        }
    }
}

/** DELETE /account/photo. */
internal class RemoveProfilePhoto(
    private val repository: AccountRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(): Result<Unit> = withContext(ioDispatcher) { repository.deletePhoto() }
}
