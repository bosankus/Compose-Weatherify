package bose.ankush.settings.presentation

import bose.ankush.network.model.PricingTier
import bose.ankush.network.model.Service
import bose.ankush.payment.presentation.PaymentUiState
import bose.ankush.settings.presentation.component.ProfileHeaderActions
import bose.ankush.settings.presentation.profile.ProfileMessage
import bose.ankush.settings.presentation.profile.ProfileState

/** Everything [SettingsScreenContent] draws. */
internal class SettingsScreenModel(
    val state: SettingsState,
    val profile: ProfileSection,
    val paymentUiState: PaymentUiState,
    val isLoggingOut: Boolean,
    val versionName: String,
    val shouldShowNotificationItem: Boolean,
)

/** Everything the profile header, its photo sheet, its remove dialog, and its toast need. */
internal class ProfileSection(
    val state: ProfileState,
    val actions: ProfileHeaderActions,
    val message: ProfileMessage? = null,
    val onRequestRemove: () -> Unit = {},
    val onDismissPhotoOptions: () -> Unit = {},
    val onConfirmRemove: () -> Unit = {},
    val onDismissRemove: () -> Unit = {},
    val onDismissMessage: () -> Unit = {},
)

/** Everything [SettingsScreenContent] can ask for. */
internal class SettingsActions(
    val onRefresh: () -> Unit = {},
    val onLogout: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onLanguage: () -> Unit = {},
    val onNotifications: () -> Unit = {},
    val onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    val onOpenLogoutDialog: () -> Unit = {},
    val onCloseLogoutDialog: () -> Unit = {},
    val onOpenWebUrl: (String) -> Unit = {},
    val onCloseWebView: () -> Unit = {},
    val premium: PremiumActions = PremiumActions(),
)

/** The premium card, the plan sheet, and the activation toast. */
internal class PremiumActions(
    val onOpenSheet: () -> Unit = {},
    val onCloseSheet: () -> Unit = {},
    val onLoadServices: () -> Unit = {},
    val onSelectService: (Service) -> Unit = {},
    val onSelectTier: (PricingTier) -> Unit = {},
    val onStartPayment: (amountPaise: Long) -> Unit = {},
    val onDismissActivationToast: () -> Unit = {},
)

/** Text of a confirmation dialog. */
internal class ConfirmCopy(
    val title: String,
    val body: String,
    val confirm: String,
)
