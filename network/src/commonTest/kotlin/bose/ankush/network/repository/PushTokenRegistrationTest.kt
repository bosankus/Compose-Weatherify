package bose.ankush.network.repository

import bose.ankush.network.api.AccountApiService
import bose.ankush.network.model.Account
import bose.ankush.network.model.AccountPhoto
import bose.ankush.network.model.ApiResponse
import bose.ankush.network.model.DeleteAccountResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PushTokenRegistrationTest {
    private val api = RecordingApi()

    @Test
    fun registerSendsTheProvidersToken() =
        runTest {
            PushTokenRegistration(api) { "device-token" }.register()
            assertEquals(listOf("device-token"), api.registered)
        }

    @Test
    fun aRotatedTokenIsSentAsGiven() =
        runTest {
            PushTokenRegistration(api) { "old" }.register("rotated")
            assertEquals(listOf("rotated"), api.registered)
        }

    @Test
    fun noTokenMeansNoCall() =
        runTest {
            val registration = PushTokenRegistration(api) { null }
            registration.register()
            registration.unregister()
            assertTrue(api.registered.isEmpty())
            assertTrue(api.unregistered.isEmpty())
        }

    @Test
    fun unregisterSendsTheProvidersToken() =
        runTest {
            PushTokenRegistration(api) { "device-token" }.unregister()
            assertEquals(listOf("device-token"), api.unregistered)
        }

    @Test
    fun aFailedCallIsSwallowed() =
        runTest {
            api.failure = IllegalStateException("offline")
            val registration = PushTokenRegistration(api) { "device-token" }
            registration.register()
            registration.unregister()
        }

    private class RecordingApi : AccountApiService {
        val registered = mutableListOf<String>()
        val unregistered = mutableListOf<String>()
        var failure: Exception? = null

        override suspend fun registerPushToken(token: String): Boolean {
            failure?.let { throw it }
            registered += token
            return true
        }

        override suspend fun unregisterPushToken(token: String): Boolean {
            failure?.let { throw it }
            unregistered += token
            return true
        }

        override suspend fun getAccount(): ApiResponse<Account> = error("unused")

        override suspend fun uploadAccountPhoto(
            bytes: ByteArray,
            contentType: String,
            fileName: String,
        ): ApiResponse<AccountPhoto> = error("unused")

        override suspend fun deleteAccountPhoto(): ApiResponse<AccountPhoto> = error("unused")

        override suspend fun deleteAccount(password: String): DeleteAccountResult = error("unused")
    }
}
