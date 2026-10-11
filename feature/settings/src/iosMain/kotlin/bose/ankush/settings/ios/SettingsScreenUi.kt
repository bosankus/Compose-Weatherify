package bose.ankush.settings.ios

/*
 * What the SwiftUI Settings page draws, already formatted and localized from this module's
 * resources, so Swift only lays it out. Lists carry stable keys for SwiftUI's ForEach.
 */

data class SettingsScreenUi(
    val strings: SettingsStrings,
    val profile: ProfileUi,
    val premium: PremiumUi,
    val plans: PlanSheetUi,
    val isLogoutConfirmVisible: Boolean,
    val isLoggingOut: Boolean,
    /** The delete-account sheet, or null when it is closed. */
    val deleteAccount: DeleteAccountUi?,
)

/**
 * The delete-account confirmation. Swift holds the password; [error] is the last attempt's
 * failure, shown under the field when [isPasswordError] and as a general message otherwise.
 */
data class DeleteAccountUi(
    val isDeleting: Boolean,
    val error: String?,
    val isPasswordError: Boolean,
    val showsPremiumWarning: Boolean,
)

data class ProfileUi(
    val email: String?,
    val photoUrl: String?,
    val isUploading: Boolean,
    val isRemoving: Boolean,
    val canChangePhoto: Boolean,
    val canRemovePhoto: Boolean,
    val isRemoveConfirmVisible: Boolean,
    val isRefreshing: Boolean,
)

/** Which premium card to show. Only [PITCH] is tappable. */
enum class PremiumStatus { ACTIVE, PROCESSING, PITCH }

data class PremiumUi(
    val status: PremiumStatus,
    val title: String,
    val subtitle: String,
)

/** The plan picker sheet. */
data class PlanSheetUi(
    val isOpen: Boolean,
    val isLoading: Boolean,
    val error: String?,
    val services: List<ServiceOptionUi>,
    val selected: SelectedPlanUi?,
)

data class ServiceOptionUi(
    val id: String,
    val name: String,
    val isSelected: Boolean,
)

data class SelectedPlanUi(
    val serviceName: String,
    val price: String,
    val duration: String,
    val features: List<PlanFeatureUi>,
    val moreFeatures: String?,
    val tiers: List<TierUi>,
)

data class PlanFeatureUi(
    val id: String,
    val text: String,
)

data class TierUi(
    val id: String,
    val price: String,
    val duration: String,
    val isSelected: Boolean,
    val isFeatured: Boolean,
)

/** One-off notices for Swift to show as a banner. */
sealed interface SettingsScreenEvent {
    data class Notice(
        val title: String,
        val message: String,
        val isError: Boolean,
    ) : SettingsScreenEvent
}
