package bose.ankush.home.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.home.domain.usecase.ObserveAccountPhotoUrl
import bose.ankush.home.domain.usecase.RefreshAccount
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The account photo shown on the home screen. Null shows the default avatar. */
internal data class AccountAvatarState(
    val photoUrl: String? = null,
)

internal sealed interface AccountAvatarIntent {
    /** The signed URL is short-lived, so the screen asks again each time it opens. */
    data object Refresh : AccountAvatarIntent
}

/**
 * State is the shared account photo, so a change made in settings shows here at once. A refresh
 * only re-fetches; a failed one leaves the last photo on screen. A newer refresh cancels an older one.
 */
internal class AccountAvatarViewModel(
    observeAccountPhotoUrl: ObserveAccountPhotoUrl,
    private val refreshAccount: RefreshAccount,
) : ViewModel() {
    val state: StateFlow<AccountAvatarState> =
        observeAccountPhotoUrl()
            .map(::AccountAvatarState)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                AccountAvatarState(),
            )

    private val refreshes = Channel<Unit>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            refreshes.receiveAsFlow().collectLatest { refreshAccount() }
        }
    }

    fun onIntent(intent: AccountAvatarIntent) {
        when (intent) {
            AccountAvatarIntent.Refresh -> refreshes.trySend(Unit)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
