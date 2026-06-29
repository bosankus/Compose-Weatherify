package bose.ankush.weatherify.presentation.strings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import bose.ankush.commonui.settings.SettingsScreenStrings
import bose.ankush.weatherify.R

@Composable
fun rememberSettingsScreenStrings() =
    SettingsScreenStrings(
        profileTitle = stringResource(R.string.profile_title),
        logout = stringResource(R.string.logout_btn_txt),
        logoutConfirmation = stringResource(R.string.logout_confirmation_txt),
        confirm = stringResource(R.string.confirm_btn_txt),
        cancel = stringResource(R.string.cancel_btn_txt),
        getPremium = stringResource(R.string.premium_get_txt),
        processing = stringResource(R.string.premium_processing_txt),
        processingDescription = stringResource(R.string.premium_processing_desc_txt),
        unlockDescription = stringResource(R.string.premium_unlock_desc_txt),
        upgradeNow = stringResource(R.string.premium_upgrade_btn_txt),
        premiumActive = stringResource(R.string.premium_active_txt),
        premiumExpires = stringResource(R.string.premium_expires_txt),
        premiumActiveStatus = stringResource(R.string.premium_active_status_txt),
        notificationsTitle = stringResource(R.string.settings_notifications_txt),
        languageTitle = stringResource(R.string.settings_language_txt),
        privacyPolicy = stringResource(R.string.legal_privacy_policy_txt),
        termsOfUse = stringResource(R.string.legal_terms_of_use_txt),
        appVersion = stringResource(R.string.legal_app_version_txt),
        backButtonDesc = stringResource(R.string.back_button_content),
        arrowRightDesc = stringResource(R.string.arrow_right_icon_content),
        premiumActivatedTitle = stringResource(R.string.premium_activated_title_txt),
        premiumActivatedMessage = stringResource(R.string.premium_activated_msg_txt),
    )
