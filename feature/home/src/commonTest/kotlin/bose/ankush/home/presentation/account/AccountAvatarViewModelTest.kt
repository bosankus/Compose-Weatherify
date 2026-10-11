package bose.ankush.home.presentation.account

import bose.ankush.home.domain.nearby.FakeAccountRepository
import bose.ankush.home.domain.usecase.ObserveAccountPhotoUrl
import bose.ankush.home.domain.usecase.RefreshAccount
import bose.ankush.network.model.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AccountAvatarViewModelTest {
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshLoadsThePhotoAndAFailedRefreshKeepsIt() =
        runTest {
            val repository =
                FakeAccountRepository(Result.success(Account(photoUrl = "https://p/1")))
            val viewModel = avatarViewModel(repository)
            assertNull(viewModel.state.value.photoUrl)

            viewModel.onIntent(AccountAvatarIntent.Refresh)
            advanceUntilIdle()
            assertEquals("https://p/1", viewModel.state.value.photoUrl)

            repository.fetched = Result.failure(IllegalStateException("offline"))
            viewModel.onIntent(AccountAvatarIntent.Refresh)
            advanceUntilIdle()
            assertEquals("https://p/1", viewModel.state.value.photoUrl)
        }

    @Test
    fun aPhotoChangedElsewhereShowsWithoutARefresh() =
        runTest {
            val repository = FakeAccountRepository()
            val viewModel = avatarViewModel(repository)

            repository.publish(Account(photoUrl = "https://p/new"))
            advanceUntilIdle()
            assertEquals("https://p/new", viewModel.state.value.photoUrl)

            repository.publish(null)
            advanceUntilIdle()
            assertNull(viewModel.state.value.photoUrl)
        }

    private fun TestScope.avatarViewModel(repository: FakeAccountRepository): AccountAvatarViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val viewModel =
            AccountAvatarViewModel(
                ObserveAccountPhotoUrl(repository),
                RefreshAccount(repository, dispatcher)
            )
        // WhileSubscribed: the screen collecting state is what keeps it live.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }
}
