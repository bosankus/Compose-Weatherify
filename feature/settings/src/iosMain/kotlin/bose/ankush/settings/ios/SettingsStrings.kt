package bose.ankush.settings.ios

import bose.ankush.settings.generated.resources.Res
import bose.ankush.settings.generated.resources.cancel_btn_txt
import bose.ankush.settings.generated.resources.close_button_content
import bose.ankush.settings.generated.resources.confirm_btn_txt
import bose.ankush.settings.generated.resources.delete_account_password_label
import bose.ankush.settings.generated.resources.delete_account_premium_warning_txt
import bose.ankush.settings.generated.resources.delete_account_records_txt
import bose.ankush.settings.generated.resources.delete_account_title_txt
import bose.ankush.settings.generated.resources.delete_account_txt
import bose.ankush.settings.generated.resources.delete_account_warning_txt
import bose.ankush.settings.generated.resources.features_txt
import bose.ankush.settings.generated.resources.legal_app_version_txt
import bose.ankush.settings.generated.resources.legal_privacy_policy_txt
import bose.ankush.settings.generated.resources.legal_terms_of_use_txt
import bose.ankush.settings.generated.resources.logout_btn_txt
import bose.ankush.settings.generated.resources.logout_confirmation_txt
import bose.ankush.settings.generated.resources.oops_txt
import bose.ankush.settings.generated.resources.premium_upgrade_btn_txt
import bose.ankush.settings.generated.resources.profile_add_photo_txt
import bose.ankush.settings.generated.resources.profile_change_photo_txt
import bose.ankush.settings.generated.resources.profile_edit_photo_content_desc
import bose.ankush.settings.generated.resources.profile_remove_photo_body_txt
import bose.ankush.settings.generated.resources.profile_remove_photo_title_txt
import bose.ankush.settings.generated.resources.profile_remove_photo_txt
import bose.ankush.settings.generated.resources.profile_title
import bose.ankush.settings.generated.resources.retry_btn_txt
import bose.ankush.settings.generated.resources.select_duration_txt
import bose.ankush.settings.generated.resources.select_plan_txt
import bose.ankush.settings.generated.resources.settings_about_header_txt
import bose.ankush.settings.generated.resources.settings_account_header_txt
import bose.ankush.settings.generated.resources.settings_language_txt
import bose.ankush.settings.generated.resources.settings_notifications_txt
import bose.ankush.settings.generated.resources.settings_preferences_header_txt
import bose.ankush.settings.generated.resources.subscribe_now_btn_txt
import org.jetbrains.compose.resources.getString

/** Fixed copy for the SwiftUI Settings page, from this module's translations. */
data class SettingsStrings(
    val profileTitle: String,
    val addPhoto: String,
    val changePhoto: String,
    val editPhoto: String,
    val removePhoto: String,
    val removePhotoTitle: String,
    val removePhotoBody: String,
    val upgrade: String,
    val preferences: String,
    val notifications: String,
    val language: String,
    val about: String,
    val privacyPolicy: String,
    val termsOfUse: String,
    val appVersion: String,
    val logout: String,
    val logoutConfirmation: String,
    val confirm: String,
    val cancel: String,
    val close: String,
    val selectPlan: String,
    val features: String,
    val selectDuration: String,
    val subscribe: String,
    val oops: String,
    val retry: String,
    val account: String,
    val deleteAccount: String,
    val deleteAccountTitle: String,
    val deleteAccountWarning: String,
    val deleteAccountPremiumWarning: String,
    val deleteAccountRecords: String,
    val currentPassword: String,
    val privacyPolicyUrl: String,
    val termsOfUseUrl: String,
)

internal suspend fun loadSettingsStrings(): SettingsStrings =
    SettingsStrings(
        profileTitle = getString(Res.string.profile_title),
        addPhoto = getString(Res.string.profile_add_photo_txt),
        changePhoto = getString(Res.string.profile_change_photo_txt),
        editPhoto = getString(Res.string.profile_edit_photo_content_desc),
        removePhoto = getString(Res.string.profile_remove_photo_txt),
        removePhotoTitle = getString(Res.string.profile_remove_photo_title_txt),
        removePhotoBody = getString(Res.string.profile_remove_photo_body_txt),
        upgrade = getString(Res.string.premium_upgrade_btn_txt),
        preferences = getString(Res.string.settings_preferences_header_txt),
        notifications = getString(Res.string.settings_notifications_txt),
        language = getString(Res.string.settings_language_txt),
        about = getString(Res.string.settings_about_header_txt),
        privacyPolicy = getString(Res.string.legal_privacy_policy_txt),
        termsOfUse = getString(Res.string.legal_terms_of_use_txt),
        appVersion = getString(Res.string.legal_app_version_txt),
        logout = getString(Res.string.logout_btn_txt),
        logoutConfirmation = getString(Res.string.logout_confirmation_txt),
        confirm = getString(Res.string.confirm_btn_txt),
        cancel = getString(Res.string.cancel_btn_txt),
        close = getString(Res.string.close_button_content),
        selectPlan = getString(Res.string.select_plan_txt),
        features = getString(Res.string.features_txt),
        selectDuration = getString(Res.string.select_duration_txt),
        subscribe = getString(Res.string.subscribe_now_btn_txt),
        oops = getString(Res.string.oops_txt),
        retry = getString(Res.string.retry_btn_txt),
        account = getString(Res.string.settings_account_header_txt),
        deleteAccount = getString(Res.string.delete_account_txt),
        deleteAccountTitle = getString(Res.string.delete_account_title_txt),
        deleteAccountWarning = getString(Res.string.delete_account_warning_txt),
        deleteAccountPremiumWarning = getString(Res.string.delete_account_premium_warning_txt),
        deleteAccountRecords = getString(Res.string.delete_account_records_txt),
        currentPassword = getString(Res.string.delete_account_password_label),
        // Same pages the Android Settings screen opens.
        privacyPolicyUrl = "https://data.androidplay.in/wfy/privacy-policy",
        termsOfUseUrl = "https://data.androidplay.in/wfy/terms-and-conditions",
    )
