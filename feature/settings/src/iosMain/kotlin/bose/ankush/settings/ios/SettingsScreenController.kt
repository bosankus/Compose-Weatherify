@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.settings.ios

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bose.ankush.payment.presentation.PaymentStage
import bose.ankush.settings.domain.ProfilePhoto
import bose.ankush.settings.generated.resources.Res
import bose.ankush.settings.generated.resources.premium_activated_msg_txt
import bose.ankush.settings.generated.resources.premium_activated_title_txt
import bose.ankush.settings.generated.resources.profile_photo_read_failed_txt
import bose.ankush.settings.generated.resources.profile_photo_remove_failed_txt
import bose.ankush.settings.generated.resources.profile_photo_removed_txt
import bose.ankush.settings.generated.resources.profile_photo_updated_txt
import bose.ankush.settings.generated.resources.profile_photo_upload_failed_txt
import bose.ankush.settings.generated.resources.profile_title
import bose.ankush.settings.presentation.SettingsIntent
import bose.ankush.settings.presentation.SettingsViewModel
import bose.ankush.settings.presentation.profile.ProfileIntent
import bose.ankush.settings.presentation.profile.ProfileMessage
import bose.ankush.settings.presentation.profile.ProfileViewModel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * The SwiftUI Settings page's entry into shared code. It owns the same [SettingsViewModel] and
 * [ProfileViewModel] the Compose Settings screen binds, folds them with the session's payment
 * state into one [SettingsScreenUi], and turns taps into their intents. Swift calls [close] when
 * the page goes away.
 */
class SettingsScreenController(
    private val session: SettingsSession,
) : KoinComponent {
    private val scope = MainScope()
    private val store = ViewModelStore()
    private val provider =
        ViewModelProvider.create(
            store,
            viewModelFactory {
                initializer { get<SettingsViewModel>() }
                initializer { get<ProfileViewModel>() }
            },
        )
    private val settings = provider[SettingsViewModel::class]
    private val profile = provider[ProfileViewModel::class]
    private val strings = MutableStateFlow<SettingsStrings?>(null)
    private val notices = Channel<SettingsScreenEvent>(Channel.BUFFERED)

    /** Null until the copy is loaded. */
    val state: StateFlow<SettingsScreenUi?> =
        combine(
            strings.filterNotNull(),
            settings.state,
            profile.state,
            session.payment,
            session.isLoggingOut,
        ) { copy, settingsState, profileState, payment, loggingOut ->
            SettingsInputs(settingsState, profileState, payment, loggingOut).toUi(copy)
        }.stateIn(scope, SharingStarted.Eagerly, null)

    /** Photo results and premium activation, as banners. */
    val events: Flow<SettingsScreenEvent> = notices.receiveAsFlow()

    init {
        scope.launch { strings.value = loadSettingsStrings() }
        profile.onIntent(ProfileIntent.Refresh)
        scope.launch { profile.messages.collect { notices.send(it.toNotice()) } }
        scope.launch { watchPayment() }
    }

    /** Pull to refresh: the account (photo, email) and the session (premium status). */
    suspend fun refresh() {
        profile.onIntent(ProfileIntent.PullToRefresh)
        session.refreshPremium()
        withTimeoutOrNull(REFRESH_START_TIMEOUT_MS) { profile.state.first { it.isRefreshing } }
        withTimeoutOrNull(REFRESH_END_TIMEOUT_MS) { profile.state.first { !it.isRefreshing } }
    }

    /** The app language changed: reload the fixed copy (per-item copy follows on the next frame). */
    fun reloadStrings() {
        scope.launch { strings.value = loadSettingsStrings() }
    }

    /** The page came back on screen; the photo URL is short-lived. */
    fun onAppear() = profile.onIntent(ProfileIntent.Refresh)

    // Profile photo

    /** A JPEG the app already re-encoded from the user's pick. */
    fun uploadPhoto(jpeg: NSData) {
        val bytes = jpeg.toByteArray()
        if (bytes.isEmpty()) {
            profile.onIntent(ProfileIntent.PhotoUnreadable)
        } else {
            profile.onIntent(ProfileIntent.PhotoPicked(ProfilePhoto(bytes)))
        }
    }

    fun photoUnreadable() = profile.onIntent(ProfileIntent.PhotoUnreadable)

    fun requestRemovePhoto() = profile.onIntent(ProfileIntent.RequestRemovePhoto)

    fun dismissRemovePhoto() = profile.onIntent(ProfileIntent.DismissRemovePhoto)

    fun confirmRemovePhoto() = profile.onIntent(ProfileIntent.ConfirmRemovePhoto)

    // Logout

    fun openLogoutConfirm() = settings.processIntent(SettingsIntent.OpenLogoutDialog)

    fun closeLogoutConfirm() = settings.processIntent(SettingsIntent.CloseLogoutDialog)

    fun confirmLogout() {
        settings.processIntent(SettingsIntent.CloseLogoutDialog)
        session.logout()
    }

    // Delete account

    fun openDeleteAccount() = settings.processIntent(SettingsIntent.OpenDeleteAccountDialog)

    /** Ignored while the request is in flight. */
    fun closeDeleteAccount() = settings.processIntent(SettingsIntent.CloseDeleteAccountDialog)

    fun deleteAccountPasswordEdited() =
        settings.processIntent(SettingsIntent.DeleteAccountPasswordEdited)

    /** On success the session ends and the app returns to sign-in on its own. */
    fun confirmDeleteAccount(password: String) =
        settings.processIntent(SettingsIntent.ConfirmDeleteAccount(password))

    // Premium plans

    fun openPlans() {
        settings.processIntent(SettingsIntent.OpenPremiumSheet)
        settings.processIntent(SettingsIntent.LoadServices)
    }

    fun closePlans() = settings.processIntent(SettingsIntent.ClosePremiumSheet)

    fun retryPlans() = settings.processIntent(SettingsIntent.LoadServices)

    fun selectService(id: String) {
        settings.state.value.serviceSubscription.services
            .firstOrNull { it.id == id }
            ?.let { settings.processIntent(SettingsIntent.SelectService(it)) }
    }

    fun selectTier(id: String) {
        settings.state.value.serviceSubscription.selectedService
            ?.pricingTiers
            ?.firstOrNull { it.id == id }
            ?.let { settings.processIntent(SettingsIntent.SelectTier(it)) }
    }

    /** Starts checkout for the selected plan; the session presents Razorpay. */
    fun subscribe() {
        val tier = settings.state.value.serviceSubscription.selectedTier ?: return
        session.startPayment(tier.getAmountInPaise().toLong())
        settings.processIntent(SettingsIntent.ClosePremiumSheet)
    }

    /** Stops every flow and clears the ViewModels. */
    fun close() {
        scope.cancel()
        store.clear()
    }

    /** Same rules as the Compose route: checkout closes the sheet; a new success shows a banner. */
    private suspend fun watchPayment() {
        var previous = session.payment.value.stage
        session.payment.map { it.stage }.collect { stage ->
            when (stage) {
                PaymentStage.CreatingOrder,
                PaymentStage.AwaitingPayment,
                PaymentStage.Verifying,
                PaymentStage.Failure,
                    -> settings.processIntent(SettingsIntent.ClosePremiumSheet)

                PaymentStage.Success ->
                    if (previous != PaymentStage.Success) {
                        notices.send(
                            SettingsScreenEvent.Notice(
                                title = getString(Res.string.premium_activated_title_txt),
                                message = getString(Res.string.premium_activated_msg_txt),
                                isError = false,
                            ),
                        )
                    }

                PaymentStage.Idle -> Unit
            }
            previous = stage
        }
    }

    private suspend fun ProfileMessage.toNotice(): SettingsScreenEvent.Notice {
        val (text, reason) =
            when (this) {
                ProfileMessage.PhotoUpdated -> Res.string.profile_photo_updated_txt to null
                ProfileMessage.PhotoRemoved -> Res.string.profile_photo_removed_txt to null
                ProfileMessage.PhotoUnreadable -> Res.string.profile_photo_read_failed_txt to null
                is ProfileMessage.UploadFailed -> Res.string.profile_photo_upload_failed_txt to reason
                is ProfileMessage.RemoveFailed -> Res.string.profile_photo_remove_failed_txt to reason
            }
        val line = getString(text)
        return SettingsScreenEvent.Notice(
            title = getString(Res.string.profile_title),
            message = reason?.let { "$line\n$it" } ?: line,
            isError = this !is ProfileMessage.PhotoUpdated && this !is ProfileMessage.PhotoRemoved,
        )
    }

    private companion object {
        const val REFRESH_START_TIMEOUT_MS = 1_000L
        const val REFRESH_END_TIMEOUT_MS = 30_000L
    }
}

private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}
