package bose.ankush.settings.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.analytics.ErrorReporter
import bose.ankush.settings.domain.ObserveAccount
import bose.ankush.settings.domain.ProfilePhoto
import bose.ankush.settings.domain.RefreshAccount
import bose.ankush.settings.domain.RemoveProfilePhoto
import bose.ankush.settings.domain.UploadProfilePhoto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The profile header: account email and photo, and changing or removing the photo.
 *
 * The email and photo come from the shared account, so the home avatar updates the moment an
 * upload finishes. Only one photo change runs at a time, and it is not cancelled by a refresh
 * or by leaving the screen's composition: the server may already have it.
 */
internal class ProfileViewModel(
    observeAccount: ObserveAccount,
    private val refreshAccount: RefreshAccount,
    private val uploadProfilePhoto: UploadProfilePhoto,
    private val removeProfilePhoto: RemoveProfilePhoto,
    private val errorReporter: ErrorReporter,
) : ViewModel() {
    private val local = MutableStateFlow(LocalState())

    val state: StateFlow<ProfileState> =
        combine(observeAccount(), local) { account, ui ->
            ProfileState(
                email = account?.email?.takeIf { it.isNotBlank() },
                photoUrl = account?.photoUrl,
                photoOperation = ui.operation,
                isRemoveConfirmVisible = ui.isRemoveConfirmVisible,
                isPhotoOptionsVisible = ui.isPhotoOptionsVisible,
                isRefreshing = ui.isRefreshing,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            ProfileState()
        )

    private val _messages = Channel<ProfileMessage>(Channel.BUFFERED)
    val messages: Flow<ProfileMessage> = _messages.receiveAsFlow()

    /** True for a pull-to-refresh, false for the quiet one on open. */
    private val refreshes = Channel<Boolean>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            refreshes.receiveAsFlow().collectLatest { userInitiated ->
                if (userInitiated) local.update { it.copy(isRefreshing = true) }
                try {
                    refreshAccount()
                } finally {
                    // Also when a newer refresh supersedes this one; that one sets it again if needed.
                    local.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Refresh -> refreshes.trySend(false)
            ProfileIntent.PullToRefresh -> refreshes.trySend(true)
            is ProfileIntent.PhotoPicked -> upload(intent.photo)
            ProfileIntent.PhotoUnreadable -> _messages.trySend(ProfileMessage.PhotoUnreadable)
            ProfileIntent.OpenPhotoOptions ->
                if (state.value.canRemovePhoto) local.update { it.copy(isPhotoOptionsVisible = true) }

            ProfileIntent.DismissPhotoOptions -> local.update { it.copy(isPhotoOptionsVisible = false) }
            ProfileIntent.RequestRemovePhoto ->
                local.update {
                    it.copy(
                        isPhotoOptionsVisible = false,
                        isRemoveConfirmVisible = state.value.canRemovePhoto
                    )
                }

            ProfileIntent.DismissRemovePhoto -> local.update { it.copy(isRemoveConfirmVisible = false) }
            ProfileIntent.ConfirmRemovePhoto -> remove()
        }
    }

    private fun upload(photo: ProfilePhoto) {
        if (!startOperation(PhotoOperation.Uploading)) return
        viewModelScope.launch {
            val result = uploadProfilePhoto(photo)
            finishOperation()
            result
                .onSuccess { _messages.trySend(ProfileMessage.PhotoUpdated) }
                .onFailure { error ->
                    report(
                        error,
                        "Profile photo upload failed",
                        mapOf("bytes" to photo.bytes.size.toString())
                    )
                    _messages.trySend(ProfileMessage.UploadFailed(error.reasonOrNull()))
                }
        }
    }

    private fun remove() {
        local.update { it.copy(isRemoveConfirmVisible = false) }
        if (!startOperation(PhotoOperation.Removing)) return
        viewModelScope.launch {
            val result = removeProfilePhoto()
            finishOperation()
            result
                .onSuccess { _messages.trySend(ProfileMessage.PhotoRemoved) }
                .onFailure { error ->
                    report(error, "Profile photo removal failed")
                    _messages.trySend(ProfileMessage.RemoveFailed(error.reasonOrNull()))
                }
        }
    }

    /** Atomically claims the single photo-operation slot. False when one is already running. */
    private fun startOperation(operation: PhotoOperation): Boolean {
        var started = false
        local.update { current ->
            // Recomputed on every attempt: update() may retry this lambda.
            started = current.operation == PhotoOperation.Idle
            if (started) current.copy(operation = operation) else current
        }
        return started
    }

    private fun report(
        error: Throwable,
        message: String,
        context: Map<String, String> = emptyMap(),
    ) {
        errorReporter.recordError(error, message, context + ("reason" to error.message.orEmpty()))
    }

    private fun Throwable.reasonOrNull(): String? = message?.trim()?.takeIf { it.isNotEmpty() }

    private fun finishOperation() {
        local.update { it.copy(operation = PhotoOperation.Idle) }
    }

    private data class LocalState(
        val operation: PhotoOperation = PhotoOperation.Idle,
        val isRemoveConfirmVisible: Boolean = false,
        val isPhotoOptionsVisible: Boolean = false,
        val isRefreshing: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
