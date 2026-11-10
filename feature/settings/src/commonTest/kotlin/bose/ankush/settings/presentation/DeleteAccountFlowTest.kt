package bose.ankush.settings.presentation

import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.analytics.ErrorReporter
import bose.ankush.network.model.Account
import bose.ankush.network.model.DeleteAccountResult
import bose.ankush.network.model.Service
import bose.ankush.network.repository.AccountRepository
import bose.ankush.network.repository.ServiceRepository
import bose.ankush.settings.domain.DeleteAccount
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountFlowTest {
    private val dispatcher = StandardTestDispatcher()
    private val accounts = FakeAccountRepository()
    private var sessionsEnded = 0

    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun aConfirmedDeletionEndsTheSessionAndClosesTheDialog() =
        runTest(dispatcher) {
            val viewModel = openDialog()

            viewModel.processIntent(SettingsIntent.ConfirmDeleteAccount("hunter22"))
            advanceUntilIdle()

            assertEquals(listOf("hunter22"), accounts.passwords)
            assertEquals(1, sessionsEnded)
            assertNull(viewModel.state.value.deleteAccount)
        }

    @Test
    fun aWrongPasswordShowsInlineAndKeepsTheSession() =
        runTest(dispatcher) {
            accounts.result = DeleteAccountResult.IncorrectPassword
            val viewModel = openDialog()

            viewModel.processIntent(SettingsIntent.ConfirmDeleteAccount("wrong"))
            advanceUntilIdle()

            assertEquals(0, sessionsEnded)
            assertEquals(
                DeleteAccountState(error = DeleteAccountError.INCORRECT_PASSWORD),
                viewModel.state.value.deleteAccount,
            )

            viewModel.processIntent(SettingsIntent.DeleteAccountPasswordEdited)
            assertEquals(DeleteAccountState(), viewModel.state.value.deleteAccount)
        }

    @Test
    fun rateLimitingAndServerErrorsKeepTheSession() =
        runTest(dispatcher) {
            val outcomes =
                mapOf(
                    DeleteAccountResult.RateLimited to DeleteAccountError.RATE_LIMITED,
                    DeleteAccountResult.InvalidRequest to DeleteAccountError.GENERIC,
                    DeleteAccountResult.Failed() to DeleteAccountError.RETRY,
                    DeleteAccountResult.Failed(IllegalStateException("offline")) to DeleteAccountError.RETRY,
                )
            for ((result, expected) in outcomes) {
                accounts.result = result
                val viewModel = openDialog()

                viewModel.processIntent(SettingsIntent.ConfirmDeleteAccount("pw"))
                advanceUntilIdle()

                assertEquals(expected, viewModel.state.value.deleteAccount?.error, "for $result")
            }
            assertEquals(0, sessionsEnded)
        }

    @Test
    fun anExpiredSessionClosesTheDialogWithoutEndingItAgain() =
        runTest(dispatcher) {
            accounts.result = DeleteAccountResult.SessionExpired
            val viewModel = openDialog()

            viewModel.processIntent(SettingsIntent.ConfirmDeleteAccount("pw"))
            advanceUntilIdle()

            assertNull(viewModel.state.value.deleteAccount)
            assertEquals(0, sessionsEnded)
        }

    @Test
    fun theDialogCannotBeClosedOrResubmittedWhileDeleting() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            accounts.gate = gate
            val viewModel = openDialog()

            viewModel.processIntent(SettingsIntent.ConfirmDeleteAccount("pw"))
            advanceUntilIdle()
            viewModel.processIntent(SettingsIntent.CloseDeleteAccountDialog)
            viewModel.processIntent(SettingsIntent.ConfirmDeleteAccount("pw"))
            advanceUntilIdle()

            assertTrue(assertNotNull(viewModel.state.value.deleteAccount).isDeleting)
            assertEquals(1, accounts.passwords.size)

            gate.complete(Unit)
            advanceUntilIdle()
            assertNull(viewModel.state.value.deleteAccount)
        }

    @Test
    fun anEmptyPasswordIsNeverSent() =
        runTest(dispatcher) {
            val viewModel = openDialog()

            viewModel.processIntent(SettingsIntent.ConfirmDeleteAccount(""))
            advanceUntilIdle()

            assertTrue(accounts.passwords.isEmpty())
            assertEquals(DeleteAccountState(), viewModel.state.value.deleteAccount)
        }

    private fun TestScope.openDialog(): SettingsViewModel {
        val viewModel =
            SettingsViewModel(
                serviceRepository = NoServices,
                analyticsTracker = NoAnalytics,
                deleteAccount = DeleteAccount(accounts, { sessionsEnded++ }, dispatcher),
                errorReporter = NoErrors,
            )
        viewModel.processIntent(SettingsIntent.OpenDeleteAccountDialog)
        advanceUntilIdle()
        return viewModel
    }

    private class FakeAccountRepository : AccountRepository {
        override val account: StateFlow<Account?> = MutableStateFlow(null)
        var result: DeleteAccountResult = DeleteAccountResult.Deleted
        var gate: CompletableDeferred<Unit>? = null
        val passwords = mutableListOf<String>()

        override suspend fun getAccount(): Result<Account> = Result.success(Account())

        override suspend fun uploadPhoto(
            bytes: ByteArray,
            contentType: String,
            fileName: String,
        ): Result<String?> = Result.success(null)

        override suspend fun deletePhoto(): Result<Unit> = Result.success(Unit)

        override suspend fun deleteAccount(password: String): DeleteAccountResult {
            passwords += password
            gate?.await()
            return result
        }

        override fun clear() = Unit
    }

    private object NoServices : ServiceRepository {
        override suspend fun getServices(
            page: Int,
            pageSize: Int,
            search: String?,
        ): Result<List<Service>> = Result.success(emptyList())
    }

    private object NoAnalytics : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) = Unit
    }

    private object NoErrors : ErrorReporter {
        override fun recordError(
            throwable: Throwable,
            message: String?,
            context: Map<String, String>,
        ) = Unit

        override fun log(message: String) = Unit

        override fun setCustomKey(
            key: String,
            value: String,
        ) = Unit

        override fun setEnabled(enabled: Boolean) = Unit
    }
}
