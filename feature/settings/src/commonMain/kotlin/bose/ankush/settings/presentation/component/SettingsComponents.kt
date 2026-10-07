package bose.ankush.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.payment.presentation.PaymentStage
import bose.ankush.payment.presentation.PaymentUiState
import bose.ankush.settings.generated.resources.Res
import bose.ankush.settings.generated.resources.arrow_right_icon_content
import bose.ankush.settings.generated.resources.legal_app_version_txt
import bose.ankush.settings.generated.resources.legal_privacy_policy_txt
import bose.ankush.settings.generated.resources.legal_terms_of_use_txt
import bose.ankush.settings.generated.resources.premium_active_status_txt
import bose.ankush.settings.generated.resources.premium_active_txt
import bose.ankush.settings.generated.resources.premium_expires_txt
import bose.ankush.settings.generated.resources.premium_get_txt
import bose.ankush.settings.generated.resources.premium_icon_content_desc
import bose.ankush.settings.generated.resources.premium_processing_desc_txt
import bose.ankush.settings.generated.resources.premium_processing_txt
import bose.ankush.settings.generated.resources.premium_unlock_desc_txt
import bose.ankush.settings.generated.resources.premium_upgrade_btn_txt
import bose.ankush.settings.generated.resources.settings_about_header_txt
import bose.ankush.settings.generated.resources.settings_language_txt
import bose.ankush.settings.generated.resources.settings_notifications_txt
import bose.ankush.settings.generated.resources.settings_preferences_header_txt
import bose.ankush.settings.util.formatDate
import org.jetbrains.compose.resources.stringResource

private const val LEGAL_PRIVACY_POLICY_URL = "https://data.androidplay.in/wfy/privacy-policy"
private const val LEGAL_TERMS_OF_USE_URL = "https://data.androidplay.in/wfy/terms-and-conditions"

private val PaymentInFlight =
    setOf(PaymentStage.CreatingOrder, PaymentStage.AwaitingPayment, PaymentStage.Verifying)

/** Active plan with its expiry, or the upgrade pitch. The pitch is the only tappable state. */
@Composable
internal fun PremiumCard(
    paymentUiState: PaymentUiState,
    onClick: () -> Unit,
) {
    val isActive = paymentUiState.isPremiumActivated || paymentUiState.stage == PaymentStage.Success
    val isProcessing = paymentUiState.loading || paymentUiState.stage in PaymentInFlight
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(SettingsCardShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            WarningYellow.copy(alpha = PREMIUM_TINT_ALPHA),
                            SettingsCardFill
                        )
                    ),
                ).border(1.dp, SettingsCardStroke, SettingsCardShape)
                .then(
                    if (!isActive &&
                        !isProcessing
                    ) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                ).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(WarningYellow.copy(alpha = PREMIUM_ICON_FILL_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = stringResource(Res.string.premium_icon_content_desc),
                    tint = WarningYellow,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        stringResource(
                            when {
                                isActive -> Res.string.premium_active_txt
                                isProcessing -> Res.string.premium_processing_txt
                                else -> Res.string.premium_get_txt
                            },
                        ),
                    color = SettingsInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = premiumSubtitle(paymentUiState, isActive, isProcessing),
                    color = SettingsInkMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
        if (isProcessing) {
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = WarningYellow,
                trackColor = SettingsInkFaint,
            )
        } else if (!isActive) {
            Spacer(modifier = Modifier.height(14.dp))
            PillButton(
                text = stringResource(Res.string.premium_upgrade_btn_txt),
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                style = PillStyle.Accent,
            )
        }
    }
}

@Composable
private fun premiumSubtitle(
    paymentUiState: PaymentUiState,
    isActive: Boolean,
    isProcessing: Boolean,
): String {
    val expiry = paymentUiState.expiryMillis
    return when {
        isActive && expiry != null -> {
            val date = remember(expiry) { formatDate(expiry) }
            stringResource(Res.string.premium_expires_txt, date)
        }

        isActive -> stringResource(Res.string.premium_active_status_txt)
        isProcessing -> stringResource(Res.string.premium_processing_desc_txt)
        else -> stringResource(Res.string.premium_unlock_desc_txt)
    }
}

@Composable
internal fun PreferencesSection(
    shouldShowNotificationItem: Boolean,
    onNotificationNavAction: () -> Unit,
    onLanguageNavAction: () -> Unit,
) {
    SectionCard(title = stringResource(Res.string.settings_preferences_header_txt)) {
        if (shouldShowNotificationItem) {
            SettingsRow(
                icon = Icons.Outlined.Notifications,
                title = stringResource(Res.string.settings_notifications_txt),
                onClick = onNotificationNavAction,
            )
            RowDivider()
        }
        SettingsRow(
            icon = Icons.Outlined.Language,
            title = stringResource(Res.string.settings_language_txt),
            onClick = onLanguageNavAction,
        )
    }
}

@Composable
internal fun AboutSection(
    versionName: String,
    onUrlClick: (String) -> Unit,
) {
    SectionCard(title = stringResource(Res.string.settings_about_header_txt)) {
        SettingsRow(
            icon = Icons.Outlined.PrivacyTip,
            title = stringResource(Res.string.legal_privacy_policy_txt),
            onClick = { onUrlClick(LEGAL_PRIVACY_POLICY_URL) },
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Outlined.Gavel,
            title = stringResource(Res.string.legal_terms_of_use_txt),
            onClick = { onUrlClick(LEGAL_TERMS_OF_USE_URL) },
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Outlined.Info,
            title = stringResource(Res.string.legal_app_version_txt),
            trailingText = versionName,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            color = SettingsInkMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(SettingsCardShape)
                    .background(SettingsCardFill)
                    .border(1.dp, SettingsCardStroke, SettingsCardShape),
            content = content,
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailingText: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick
                    ) else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SettingsInkMuted,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            color = SettingsInk,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        when {
            trailingText != null -> Text(
                text = trailingText,
                color = SettingsInkMuted,
                fontSize = 14.sp
            )

            onClick != null ->
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.arrow_right_icon_content),
                    tint = SettingsInkMuted,
                )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        thickness = 1.dp,
        color = SettingsCardStroke
    )
}

private const val PREMIUM_TINT_ALPHA = 0.16f
private const val PREMIUM_ICON_FILL_ALPHA = 0.16f
