package bose.ankush.settings.ios

import bose.ankush.network.model.Service
import bose.ankush.payment.presentation.PaymentStage
import bose.ankush.payment.presentation.PaymentUiState
import bose.ankush.settings.generated.resources.Res
import bose.ankush.settings.generated.resources.delete_account_error_generic_txt
import bose.ankush.settings.generated.resources.delete_account_error_password_txt
import bose.ankush.settings.generated.resources.delete_account_error_rate_limited_txt
import bose.ankush.settings.generated.resources.delete_account_error_retry_txt
import bose.ankush.settings.generated.resources.more_features_txt
import bose.ankush.settings.generated.resources.plan_duration_txt
import bose.ankush.settings.generated.resources.premium_active_status_txt
import bose.ankush.settings.generated.resources.premium_active_txt
import bose.ankush.settings.generated.resources.premium_expires_txt
import bose.ankush.settings.generated.resources.premium_get_txt
import bose.ankush.settings.generated.resources.premium_processing_desc_txt
import bose.ankush.settings.generated.resources.premium_processing_txt
import bose.ankush.settings.generated.resources.premium_unlock_desc_txt
import bose.ankush.settings.presentation.DeleteAccountError
import bose.ankush.settings.presentation.DeleteAccountState
import bose.ankush.settings.presentation.ServiceSubscriptionState
import bose.ankush.settings.presentation.SettingsState
import bose.ankush.settings.presentation.component.hasPremium
import bose.ankush.settings.presentation.profile.PhotoOperation
import bose.ankush.settings.presentation.profile.ProfileState
import bose.ankush.settings.util.formatDate
import org.jetbrains.compose.resources.getString

/** Inputs of one Settings frame. */
internal class SettingsInputs(
    val settings: SettingsState,
    val profile: ProfileState,
    val payment: PaymentUiState,
    val isLoggingOut: Boolean,
)

internal suspend fun SettingsInputs.toUi(strings: SettingsStrings): SettingsScreenUi =
    SettingsScreenUi(
        strings = strings,
        profile =
            ProfileUi(
                email = profile.email,
                photoUrl = profile.photoUrl,
                isUploading = profile.photoOperation == PhotoOperation.Uploading,
                isRemoving = profile.photoOperation == PhotoOperation.Removing,
                canChangePhoto = !profile.isBusy,
                canRemovePhoto = profile.canRemovePhoto,
                isRemoveConfirmVisible = profile.isRemoveConfirmVisible,
                isRefreshing = profile.isRefreshing,
            ),
        premium = payment.toPremiumUi(),
        plans = settings.serviceSubscription.toPlanSheet(isOpen = settings.showPremiumBottomSheet),
        isLogoutConfirmVisible = settings.showLogoutDialog,
        isLoggingOut = isLoggingOut,
        deleteAccount = settings.deleteAccount?.toUi(isPremium = payment.hasPremium),
    )

private suspend fun DeleteAccountState.toUi(isPremium: Boolean): DeleteAccountUi =
    DeleteAccountUi(
        isDeleting = isDeleting,
        error =
            when (error) {
                DeleteAccountError.INCORRECT_PASSWORD -> getString(Res.string.delete_account_error_password_txt)
                DeleteAccountError.RATE_LIMITED -> getString(Res.string.delete_account_error_rate_limited_txt)
                DeleteAccountError.GENERIC -> getString(Res.string.delete_account_error_generic_txt)
                DeleteAccountError.RETRY -> getString(Res.string.delete_account_error_retry_txt)
                null -> null
            },
        isPasswordError = error == DeleteAccountError.INCORRECT_PASSWORD,
        showsPremiumWarning = isPremium,
    )

private val PaymentInFlight =
    setOf(PaymentStage.CreatingOrder, PaymentStage.AwaitingPayment, PaymentStage.Verifying)

/** Same three states as the Android premium card. */
private suspend fun PaymentUiState.toPremiumUi(): PremiumUi {
    val active = hasPremium
    val processing = loading || stage in PaymentInFlight
    val expiry = expiryMillis
    return when {
        active ->
            PremiumUi(
                status = PremiumStatus.ACTIVE,
                title = getString(Res.string.premium_active_txt),
                subtitle =
                    expiry?.let { getString(Res.string.premium_expires_txt, formatDate(it)) }
                        ?: getString(Res.string.premium_active_status_txt),
            )

        processing ->
            PremiumUi(
                status = PremiumStatus.PROCESSING,
                title = getString(Res.string.premium_processing_txt),
                subtitle = getString(Res.string.premium_processing_desc_txt),
            )

        else ->
            PremiumUi(
                status = PremiumStatus.PITCH,
                title = getString(Res.string.premium_get_txt),
                subtitle = getString(Res.string.premium_unlock_desc_txt),
            )
    }
}

private suspend fun ServiceSubscriptionState.toPlanSheet(isOpen: Boolean): PlanSheetUi =
    PlanSheetUi(
        isOpen = isOpen,
        isLoading = isLoading,
        error = error,
        services =
            services.map { service ->
                ServiceOptionUi(
                    id = service.id,
                    name = service.displayName,
                    isSelected = service.id == selectedService?.id,
                )
            },
        selected = selectedService?.let { service -> selectedPlan(service) },
    )

private suspend fun ServiceSubscriptionState.selectedPlan(service: Service): SelectedPlanUi? {
    val tier = selectedTier ?: return null
    val features = service.features.sortedBy { it.displayOrder }
    return SelectedPlanUi(
        serviceName = service.displayName,
        price = tier.getDisplayPrice(),
        duration = getString(Res.string.plan_duration_txt, tier.getDisplayDuration()),
        features =
            features
                .take(FEATURE_PREVIEW)
                .map { PlanFeatureUi(id = it.id, text = it.description) },
        moreFeatures =
            (features.size - FEATURE_PREVIEW)
                .takeIf { it > 0 }
                ?.let { getString(Res.string.more_features_txt, it) },
        // A single tier needs no picker, as on Android.
        tiers =
            service.pricingTiers
                .takeIf { it.size > 1 }
                .orEmpty()
                .sortedBy { it.displayOrder }
                .map {
                    TierUi(
                        id = it.id,
                        price = it.getDisplayPrice(),
                        duration = it.getDisplayDuration(),
                        isSelected = it.id == tier.id,
                        isFeatured = it.isFeatured,
                    )
                },
    )
}

private const val FEATURE_PREVIEW = 8
