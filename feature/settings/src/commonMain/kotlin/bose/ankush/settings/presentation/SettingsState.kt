package bose.ankush.settings.presentation

internal data class SettingsState(
    val showPremiumBottomSheet: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val showPremiumActivationToast: Boolean = false,
    val currentWebUrl: String? = null,
    val serviceSubscription: ServiceSubscriptionState = ServiceSubscriptionState(),
    /** The delete-account dialog, or null when it is closed. */
    val deleteAccount: DeleteAccountState? = null,
)

/** The password is held by the dialog itself, never in this state. */
internal data class DeleteAccountState(
    val isDeleting: Boolean = false,
    val error: DeleteAccountError? = null,
)

/** Why the last attempt didn't delete the account. The user is still signed in for all of them. */
internal enum class DeleteAccountError {
    /** Shown inline under the password field. */
    INCORRECT_PASSWORD,
    RATE_LIMITED,

    /** The request itself was rejected (400): nothing the user can fix. */
    GENERIC,

    /** Server error or no connection: the account may still exist, so try again. */
    RETRY,
}
