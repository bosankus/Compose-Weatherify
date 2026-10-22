package bose.ankush.settings.presentation.profile

import bose.ankush.analytics.ErrorReporter
import bose.ankush.network.model.Account
import bose.ankush.network.model.DeleteAccountResult
import bose.ankush.network.repository.AccountRepository
import bose.ankush.settings.domain.ObserveAccount
import bose.ankush.settings.domain.ProfilePhoto
import bose.ankush.settings.domain.RefreshAccount
import bose.ankush.settings.domain.RemoveProfilePhoto
import bose.ankush.settings.domain.UploadProfilePhoto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val reports = mutableListOf<String>()
    private val reporter =
        object : ErrorReporter {
            override fun recordError(
                throwable: Throwable,
                message: String?,
                context: Map<String, String>,
            ) {
                reports += message.orEmpty()
            }

            override fun log(message: String) = Unit

            override fun setCustomKey(
                key: String,
                value: String,
            ) = Unit

            override fun setEnabled(enabled: Boolean) = Unit
        }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshShowsTheAccount() =
        runTest {
            val repository = FakeAccountRepository(Account("a@b.c", "https://p/1"))
            val (viewModel, _) = profileViewModel(repository)

            viewModel.onIntent(ProfileIntent.Refresh)
            advanceUntilIdle()

            assertEquals("a@b.c", viewModel.state.value.email)
            assertEquals("https://p/1", viewModel.state.value.photoUrl)
        }

    @Test
    fun anUploadShowsProgressThenThePhotoAndAMessage() =
        runTest {
            val repository = FakeAccountRepository(Account("a@b.c"))
            val gate = CompletableDeferred<Unit>()
            repository.uploadGate = gate
            val (viewModel, messages) = profileViewModel(repository)

            viewModel.onIntent(ProfileIntent.PhotoPicked(ProfilePhoto(byteArrayOf(1, 2, 3))))
            advanceUntilIdle()
            assertEquals(PhotoOperation.Uploading, viewModel.state.value.photoOperation)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(PhotoOperation.Idle, viewModel.state.value.photoOperation)
            assertEquals("https://p/uploaded", viewModel.state.value.photoUrl)
            assertEquals(listOf(ProfileMessage.PhotoUpdated), messages)
        }

    @Test
    fun aSecondPickWhileUploadingIsIgnored() =
        runTest {
            val repository = FakeAccountRepository(Account("a@b.c"))
            repository.uploadGate = CompletableDeferred()
            val (viewModel, _) = profileViewModel(repository)

            viewModel.onIntent(ProfileIntent.PhotoPicked(ProfilePhoto(byteArrayOf(1))))
            viewModel.onIntent(ProfileIntent.PhotoPicked(ProfilePhoto(byteArrayOf(2))))
            advanceUntilIdle()

            assertEquals(1, repository.uploads)
        }

    @Test
    fun anOversizedPhotoIsNeverSent() =
        runTest {
            val repository = FakeAccountRepository(Account("a@b.c"))
            val (viewModel, messages) = profileViewModel(repository)

            viewModel.onIntent(ProfileIntent.PhotoPicked(ProfilePhoto(ByteArray(ProfilePhoto.MAX_BYTES + 1))))
            advanceUntilIdle()

            assertEquals(0, repository.uploads)
            assertEquals(1, messages.size)
            assertTrue(messages.single() is ProfileMessage.UploadFailed)
            assertEquals(PhotoOperation.Idle, viewModel.state.value.photoOperation)
        }

    @Test
    fun photoOptionsOpenOnlyWithAPhotoAndRemoveMovesToTheConfirmation() =
        runTest {
            val repository = FakeAccountRepository(Account("a@b.c"))
            val (viewModel, _) = profileViewModel(repository)
            viewModel.onIntent(ProfileIntent.Refresh)
            advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.OpenPhotoOptions)
            assertFalse(viewModel.state.value.isPhotoOptionsVisible)

            repository.publish(Account("a@b.c", "https://p/1"))
            advanceUntilIdle()
            viewModel.onIntent(ProfileIntent.OpenPhotoOptions)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isPhotoOptionsVisible)

            viewModel.onIntent(ProfileIntent.RequestRemovePhoto)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isPhotoOptionsVisible)
            assertTrue(viewModel.state.value.isRemoveConfirmVisible)

            viewModel.onIntent(ProfileIntent.DismissRemovePhoto)
            viewModel.onIntent(ProfileIntent.OpenPhotoOptions)
            viewModel.onIntent(ProfileIntent.DismissPhotoOptions)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isPhotoOptionsVisible)
        }

    @Test
    fun removingNeedsAPhotoAndAConfirmation() =
        runTest {
            val repository = FakeAccountRepository(Account("a@b.c"))
            val (viewModel, messages) = profileViewModel(repository)
            viewModel.onIntent(ProfileIntent.Refresh)
            advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.RequestRemovePhoto)
            assertFalse(viewModel.state.value.isRemoveConfirmVisible)

            repository.publish(Account("a@b.c", "https://p/1"))
            advanceUntilIdle()
            viewModel.onIntent(ProfileIntent.RequestRemovePhoto)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isRemoveConfirmVisible)

            viewModel.onIntent(ProfileIntent.ConfirmRemovePhoto)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isRemoveConfirmVisible)
            assertNull(viewModel.state.value.photoUrl)
            assertEquals(listOf(ProfileMessage.PhotoRemoved), messages)
        }

    @Test
    fun aFailedUploadKeepsTheOldPhoto() =
        runTest {
            val repository = FakeAccountRepository(Account("a@b.c", "https://p/old"))
            repository.uploadResult =
                Result.failure(IllegalStateException("Photo exceeds 5MB limit"))
            val (viewModel, messages) = profileViewModel(repository)
            viewModel.onIntent(ProfileIntent.Refresh)
            advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.PhotoPicked(ProfilePhoto(byteArrayOf(1))))
            advanceUntilIdle()

            assertEquals("https://p/old", viewModel.state.value.photoUrl)
            // The server's own reason reaches the screen and the error report.
            assertEquals(listOf(ProfileMessage.UploadFailed("Photo exceeds 5MB limit")), messages)
            assertEquals(listOf("Profile photo upload failed"), reports)
        }

    @Test
    fun onlyAPullShowsTheRefreshIndicatorAndItClearsWhenDone() =
        runTest {
            val repository = FakeAccountRepository(Account("a@b.c", "https://p/1"))
            val gate = CompletableDeferred<Unit>()
            repository.fetchGate = gate
            val (viewModel, _) = profileViewModel(repository)

            viewModel.onIntent(ProfileIntent.Refresh)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isRefreshing)

            viewModel.onIntent(ProfileIntent.PullToRefresh)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isRefreshing)

            gate.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isRefreshing)
            assertEquals("https://p/1", viewModel.state.value.photoUrl)
        }

    private fun TestScope.profileViewModel(
        repository: FakeAccountRepository,
    ): Pair<ProfileViewModel, List<ProfileMessage>> {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val viewModel =
            ProfileViewModel(
                observeAccount = ObserveAccount(repository),
                refreshAccount = RefreshAccount(repository, dispatcher),
                uploadProfilePhoto = UploadProfilePhoto(repository, dispatcher),
                removeProfilePhoto = RemoveProfilePhoto(repository, dispatcher),
                errorReporter = reporter,
            )
        val messages = mutableListOf<ProfileMessage>()
        val collector = UnconfinedTestDispatcher(testScheduler)
        backgroundScope.launch(collector) { viewModel.state.collect {} }
        backgroundScope.launch(collector) { viewModel.messages.collect { messages += it } }
        return viewModel to messages
    }

    private class FakeAccountRepository(
        private val server: Account,
    ) : AccountRepository {
        private val _account = MutableStateFlow<Account?>(null)
        override val account: StateFlow<Account?> = _account
        var uploadGate: CompletableDeferred<Unit>? = null
        var fetchGate: CompletableDeferred<Unit>? = null
        var uploadResult: Result<String?> = Result.success("https://p/uploaded")
        var uploads = 0
            private set

        override suspend fun getAccount(): Result<Account> {
            fetchGate?.await()
            return Result.success(server).onSuccess { _account.value = it }
        }

        override suspend fun uploadPhoto(
            bytes: ByteArray,
            contentType: String,
            fileName: String,
        ): Result<String?> {
            uploads++
            uploadGate?.await()
            return uploadResult.onSuccess { url ->
                _account.value = (_account.value ?: server).copy(photoUrl = url)
            }
        }

        override suspend fun deletePhoto(): Result<Unit> {
            _account.value = _account.value?.copy(photoUrl = null)
            return Result.success(Unit)
        }

        override suspend fun deleteAccount(password: String): DeleteAccountResult = DeleteAccountResult.Deleted

        override fun clear() {
            _account.value = null
        }

        fun publish(account: Account) {
            _account.value = account
        }
    }
}
