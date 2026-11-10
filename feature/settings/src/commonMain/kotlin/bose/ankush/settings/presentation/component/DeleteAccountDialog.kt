package bose.ankush.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.settings.generated.resources.Res
import bose.ankush.settings.generated.resources.cancel_btn_txt
import bose.ankush.settings.generated.resources.delete_account_error_generic_txt
import bose.ankush.settings.generated.resources.delete_account_error_password_txt
import bose.ankush.settings.generated.resources.delete_account_error_rate_limited_txt
import bose.ankush.settings.generated.resources.delete_account_error_retry_txt
import bose.ankush.settings.generated.resources.delete_account_hide_password_content_desc
import bose.ankush.settings.generated.resources.delete_account_password_label
import bose.ankush.settings.generated.resources.delete_account_premium_warning_txt
import bose.ankush.settings.generated.resources.delete_account_records_txt
import bose.ankush.settings.generated.resources.delete_account_show_password_content_desc
import bose.ankush.settings.generated.resources.delete_account_title_txt
import bose.ankush.settings.generated.resources.delete_account_txt
import bose.ankush.settings.generated.resources.delete_account_warning_txt
import bose.ankush.settings.presentation.DeleteAccountError
import bose.ankush.settings.presentation.DeleteAccountState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Confirms account deletion with the current password. The password lives only in this dialog's
 * composition, so it is gone as soon as the dialog closes. Nothing can be dismissed while the
 * request is in flight.
 */
@Composable
internal fun DeleteAccountDialog(
    state: DeleteAccountState,
    isPremium: Boolean,
    onPasswordEdited: () -> Unit,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val canConfirm = password.isNotEmpty() && !state.isDeleting
    val confirm = { if (canConfirm) onConfirm(password) }

    AlertDialog(
        onDismissRequest = { if (!state.isDeleting) onDismiss() },
        containerColor = SettingsSurface,
        titleContentColor = SettingsInk,
        textContentColor = SettingsInkMuted,
        shape = SettingsCardShape,
        title = {
            Text(
                text = stringResource(Res.string.delete_account_title_txt),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(Res.string.delete_account_warning_txt))
                if (isPremium) {
                    Text(
                        text = stringResource(Res.string.delete_account_premium_warning_txt),
                        color = SettingsDanger
                    )
                }
                Text(text = stringResource(Res.string.delete_account_records_txt), fontSize = 13.sp)
                val inlineError = state.error?.let { stringResource(it.message) }
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (state.error != null) onPasswordEdited()
                    },
                    label = { Text(stringResource(Res.string.delete_account_password_label)) },
                    singleLine = true,
                    enabled = !state.isDeleting,
                    isError = state.error == DeleteAccountError.INCORRECT_PASSWORD,
                    supportingText = inlineError?.let { { Text(it) } },
                    visualTransformation =
                        if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector =
                                    if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription =
                                    stringResource(
                                        if (isPasswordVisible) {
                                            Res.string.delete_account_hide_password_content_desc
                                        } else {
                                            Res.string.delete_account_show_password_content_desc
                                        },
                                    ),
                            )
                        }
                    },
                    colors = passwordFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = canConfirm) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SettingsDanger,
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.delete_account_txt),
                        color = SettingsDanger.copy(alpha = if (canConfirm || state.isDeleting) 1f else DISABLED_ALPHA),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isDeleting) {
                Text(stringResource(Res.string.cancel_btn_txt), color = SettingsInk)
            }
        },
    )
}

private val DeleteAccountError.message: StringResource
    get() =
        when (this) {
            DeleteAccountError.INCORRECT_PASSWORD -> Res.string.delete_account_error_password_txt
            DeleteAccountError.RATE_LIMITED -> Res.string.delete_account_error_rate_limited_txt
            DeleteAccountError.GENERIC -> Res.string.delete_account_error_generic_txt
            DeleteAccountError.RETRY -> Res.string.delete_account_error_retry_txt
        }

@Composable
private fun passwordFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = SettingsInk,
        unfocusedTextColor = SettingsInk,
        disabledTextColor = SettingsInkMuted,
        focusedBorderColor = SettingsInk,
        unfocusedBorderColor = SettingsCardStroke,
        focusedLabelColor = SettingsInk,
        unfocusedLabelColor = SettingsInkMuted,
        cursorColor = SettingsInk,
        focusedTrailingIconColor = SettingsInkMuted,
        unfocusedTrailingIconColor = SettingsInkMuted,
        focusedSupportingTextColor = SettingsDanger,
        unfocusedSupportingTextColor = SettingsDanger,
        errorBorderColor = SettingsDanger,
        errorLabelColor = SettingsDanger,
        errorSupportingTextColor = SettingsDanger,
        errorCursorColor = SettingsDanger,
        errorTextColor = SettingsInk,
        errorTrailingIconColor = SettingsInkMuted,
    )

private const val DISABLED_ALPHA = 0.4f
