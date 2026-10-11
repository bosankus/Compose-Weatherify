package bose.ankush.network.repository

import bose.ankush.network.api.AccountApiService
import bose.ankush.network.model.Account
import bose.ankush.network.model.AccountPhoto
import bose.ankush.network.model.ApiResponse
import bose.ankush.network.model.DeleteAccountResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountRepositoryImplTest {
    private val api = FakeAccountApi()
    private val photoStore = RecordingPhotoStore()
    private val repository = AccountRepositoryImpl(api, photoStore)

    @Test
    fun fetchPublishesTheAccountAndTreatsABlankPhotoAsNone() =
        runTest {
            api.account =
                ApiResponse(status = true, message = "ok", data = Account("a@b.c", photoUrl = " "))

            repository.getAccount()

            assertEquals("a@b.c", repository.account.value?.email)
            assertNull(repository.account.value?.photoUrl)
        }

    @Test
    fun uploadPublishesTheNewPhotoAndKeepsTheEmail() =
        runTest {
            repository.getAccount()

            val url =
                repository.uploadPhoto(byteArrayOf(1, 2), "image/jpeg", "photo.jpg").getOrThrow()

            assertEquals("https://signed/new", url)
            assertEquals(Account("a@b.c", "https://signed/new"), repository.account.value)
            assertEquals("image/jpeg", api.uploadedType)
        }

    @Test
    fun uploadStoresTheBytesBeforePublishingTheNewUrl() =
        runTest {
            repository.getAccount()
            photoStore.onStore =
                { photoStore.publishedWhenStored = repository.account.value?.photoUrl }

            repository.uploadPhoto(byteArrayOf(7, 8), "image/jpeg", "photo.jpg").getOrThrow()

            assertEquals("https://signed/new", photoStore.storedUrl)
            assertContentEquals(byteArrayOf(7, 8), photoStore.storedBytes)
            assertNull(photoStore.publishedWhenStored)
        }

    @Test
    fun aFailedUploadLeavesTheCachedPhoto() =
        runTest {
            api.account = ApiResponse(
                status = true,
                message = "ok",
                data = Account("a@b.c", "https://signed/old")
            )
            repository.getAccount()
            api.upload = ApiResponse(status = false, message = "Photo exceeds 5MB limit")

            val result = repository.uploadPhoto(byteArrayOf(1), "image/jpeg", "photo.jpg")

            assertTrue(result.isFailure)
            assertEquals("Photo exceeds 5MB limit", result.exceptionOrNull()?.message)
            assertEquals("https://signed/old", repository.account.value?.photoUrl)
            assertNull(photoStore.storedUrl)
        }

    @Test
    fun deleteClearsThePhotoAndClearForgetsTheAccount() =
        runTest {
            api.account = ApiResponse(
                status = true,
                message = "ok",
                data = Account("a@b.c", "https://signed/old")
            )
            repository.getAccount()

            repository.deletePhoto().getOrThrow()
            assertEquals(Account("a@b.c", null), repository.account.value)
            assertEquals(listOf("https://signed/old"), photoStore.removed)

            repository.clear()
            assertNull(repository.account.value)
            assertTrue(photoStore.cleared)
        }

    @Test
    fun deletingTheAccountForgetsTheCachedAccountAndPhoto() =
        runTest {
            repository.getAccount()

            val result = repository.deleteAccount("secret")

            assertEquals(DeleteAccountResult.Deleted, result)
            assertNull(repository.account.value)
            assertTrue(photoStore.cleared)
        }

    @Test
    fun aWrongPasswordKeepsTheCachedAccount() =
        runTest {
            repository.getAccount()
            api.deleteResult = DeleteAccountResult.IncorrectPassword

            val result = repository.deleteAccount("wrong")

            assertEquals(DeleteAccountResult.IncorrectPassword, result)
            assertEquals("a@b.c", repository.account.value?.email)
            assertFalse(photoStore.cleared)
        }

    @Test
    fun aTransportFailureIsReportedAndKeepsTheCachedAccount() =
        runTest {
            repository.getAccount()
            api.deleteError = IllegalStateException("offline")

            val result = repository.deleteAccount("secret")

            assertIs<DeleteAccountResult.Failed>(result)
            assertEquals("a@b.c", repository.account.value?.email)
            assertFalse(photoStore.cleared)
        }

    private class RecordingPhotoStore : AccountPhotoStore {
        var storedUrl: String? = null
        var storedBytes: ByteArray? = null
        var publishedWhenStored: String? = null
        var onStore: () -> Unit = {}
        val removed = mutableListOf<String>()
        var cleared = false

        override suspend fun store(
            photoUrl: String,
            bytes: ByteArray,
        ) {
            storedUrl = photoUrl
            storedBytes = bytes
            onStore()
        }

        override fun remove(photoUrl: String) {
            removed += photoUrl
        }

        override fun clear() {
            cleared = true
        }
    }

    private class FakeAccountApi : AccountApiService {
        var account = ApiResponse(status = true, message = "ok", data = Account("a@b.c"))
        var upload =
            ApiResponse(status = true, message = "ok", data = AccountPhoto("https://signed/new"))
        var uploadedType: String? = null

        override suspend fun getAccount(): ApiResponse<Account> = account

        override suspend fun uploadAccountPhoto(
            bytes: ByteArray,
            contentType: String,
            fileName: String,
        ): ApiResponse<AccountPhoto> {
            uploadedType = contentType
            return upload
        }

        override suspend fun deleteAccountPhoto(): ApiResponse<AccountPhoto> =
            ApiResponse(status = true, message = "ok", data = AccountPhoto(null))

        var deleteResult: DeleteAccountResult = DeleteAccountResult.Deleted
        var deleteError: Exception? = null

        override suspend fun deleteAccount(password: String): DeleteAccountResult {
            deleteError?.let { throw it }
            return deleteResult
        }
    }
}
