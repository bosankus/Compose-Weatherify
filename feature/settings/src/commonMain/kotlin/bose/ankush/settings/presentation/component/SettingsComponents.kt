package bose.ankush.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import bose.ankush.settings.generated.resources.settings_language_txt
import bose.ankush.settings.generated.resources.settings_notifications_txt
import bose.ankush.settings.util.formatDate
import org.jetbrains.compose.resources.stringResource

private const val LEGAL_PRIVACY_POLICY_URL = "https://data.androidplay.in/wfy/privacy-policy"
private const val LEGAL_TERMS_OF_USE_URL = "https://data.androidplay.in/wfy/terms-and-conditions"

@Composable
internal fun PremiumCard(
    paymentUiState: PaymentUiState,
    onClick: () -> Unit,
) {
    val isPremiumActive =
        paymentUiState.isPremiumActivated || paymentUiState.stage == PaymentStage.Success
    val cardColors =
        if (isPremiumActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (!isPremiumActive) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = cardColors,
    ) {
        if (isPremiumActive) {
            SubscribedPremiumCard(paymentUiState)
        } else {
            UnsubscribedPremiumCard(paymentUiState, onClick)
        }
    }
}

@Composable
private fun UnsubscribedPremiumCard(
    paymentUiState: PaymentUiState,
    onClick: () -> Unit,
) {
    val loadingStages =
        remember {
            listOf(
                PaymentStage.CreatingOrder,
                PaymentStage.AwaitingPayment,
                PaymentStage.Verifying,
            )
        }
    val isLoading = paymentUiState.loading || paymentUiState.stage in loadingStages

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Icon(
            imageVector = Icons.Outlined.WorkspacePremium,
            contentDescription = stringResource(Res.string.premium_icon_content_desc),
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text =
                if (isLoading) {
                    stringResource(Res.string.premium_processing_txt)
                } else {
                    stringResource(Res.string.premium_get_txt)
                },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                if (isLoading) {
                    stringResource(Res.string.premium_processing_desc_txt)
                } else {
                    stringResource(Res.string.premium_unlock_desc_txt)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onClick,
            enabled = !isLoading,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
        ) {
            Text(
                if (isLoading) {
                    stringResource(Res.string.premium_processing_txt)
                } else {
                    stringResource(Res.string.premium_upgrade_btn_txt)
                },
            )
        }
    }
}

@Composable
private fun SubscribedPremiumCard(paymentUiState: PaymentUiState) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.WorkspacePremium,
            contentDescription = stringResource(Res.string.premium_icon_content_desc),
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.premium_active_txt),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val expiryTop = paymentUiState.expiryMillis
        if (expiryTop != null) {
            val dateStr = remember(expiryTop) { formatDate(expiryTop) }
            Text(
                text = stringResource(Res.string.premium_expires_txt, dateStr),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
        } else {
            Text(
                text = stringResource(Res.string.premium_active_status_txt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun SettingsSection(
    shouldShowNotificationItem: Boolean,
    onNotificationNavAction: () -> Unit,
    onLanguageNavAction: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(vertical = 8.dp),
    ) {
        if (shouldShowNotificationItem) {
            SettingsItem(
                icon = Icons.Outlined.Notifications,
                title = stringResource(Res.string.settings_notifications_txt),
                onClick = onNotificationNavAction,
            )
        }
        SettingsItem(
            icon = Icons.Outlined.Language,
            title = stringResource(Res.string.settings_language_txt),
            onClick = onLanguageNavAction,
        )
    }
}

@Composable
internal fun LegalSection(
    versionName: String,
    onUrlClick: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(vertical = 8.dp),
    ) {
        SettingsItem(
            icon = Icons.Outlined.PrivacyTip,
            title = stringResource(Res.string.legal_privacy_policy_txt),
            onClick = { onUrlClick(LEGAL_PRIVACY_POLICY_URL) },
        )
        SettingsItem(
            icon = Icons.Outlined.Gavel,
            title = stringResource(Res.string.legal_terms_of_use_txt),
            onClick = { onUrlClick(LEGAL_TERMS_OF_USE_URL) },
        )
        SettingsItem(
            icon = Icons.Outlined.Info,
            title = stringResource(Res.string.legal_app_version_txt),
            trailingContent = {
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            },
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(Res.string.arrow_right_icon_content),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}
