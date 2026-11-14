package bose.ankush.navigation.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import bose.ankush.commonui.theme.NightCardFill
import bose.ankush.commonui.theme.NightCardStroke
import bose.ankush.commonui.theme.NightDanger
import bose.ankush.commonui.theme.NightInk
import bose.ankush.commonui.theme.NightInkFaint
import bose.ankush.commonui.theme.NightInkMuted
import bose.ankush.commonui.theme.NightPillShape
import bose.ankush.commonui.theme.NightSurface
import bose.ankush.navigation.generated.resources.Res
import bose.ankush.navigation.generated.resources.exit_btn_txt
import bose.ankush.navigation.generated.resources.permission_allow_btn_txt
import bose.ankush.navigation.generated.resources.permission_denied_txt
import bose.ankush.navigation.generated.resources.permission_granted_txt
import bose.ankush.navigation.generated.resources.permission_location_reason_txt
import bose.ankush.navigation.generated.resources.permission_location_title_txt
import bose.ankush.navigation.generated.resources.permission_not_now_btn_txt
import bose.ankush.navigation.generated.resources.permission_notification_reason_txt
import bose.ankush.navigation.generated.resources.permission_notification_title_txt
import bose.ankush.navigation.generated.resources.permissions_dialog_body_txt
import bose.ankush.navigation.generated.resources.permissions_dialog_title_txt
import org.jetbrains.compose.resources.stringResource

/** Where one runtime permission stands, as the permissions list shows it. */
internal enum class PermissionStatus {
    /** Not granted and not refused in this session yet — the row offers to ask. */
    NotRequested,
    Granted,

    /** Refused — the system won't reliably ask again, so the row sends the user to Settings. */
    Denied,
}

internal enum class AppPermission { Location, Notifications }

internal data class PermissionRow(
    val permission: AppPermission,
    val status: PermissionStatus,
)

private val CardShape = RoundedCornerShape(28.dp)
private val GroupShape = RoundedCornerShape(20.dp)

/**
 * One card listing every runtime permission the app needs, each with its own action on the right:
 * "Allow" asks the system, "Denied" opens Settings, and a granted row just shows a check.
 *
 * Location is required, so while it's missing the card can't be dismissed — the only way out is
 * [onExit]. Once location is granted, anything left is optional and [onDismiss] ("Not now") closes it.
 */
@Composable
internal fun PermissionsDialog(
    rows: List<PermissionRow>,
    onAllow: (AppPermission) -> Unit,
    onOpenSettings: (AppPermission) -> Unit,
    onDismiss: () -> Unit,
    onExit: () -> Unit,
) {
    val isLocationMissing =
        rows.any { it.permission == AppPermission.Location && it.status != PermissionStatus.Granted }

    Dialog(
        onDismissRequest = { if (!isLocationMissing) onDismiss() },
        properties =
            DialogProperties(
                dismissOnBackPress = !isLocationMissing,
                dismissOnClickOutside = !isLocationMissing,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(NightSurface)
                    .border(1.dp, NightCardStroke, CardShape)
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.permissions_dialog_title_txt),
                color = NightInk,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.permissions_dialog_body_txt),
                color = NightInkMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(20.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(GroupShape)
                        .background(NightCardFill),
            ) {
                rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(start = 52.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(NightInkFaint),
                        )
                    }
                    PermissionListItem(
                        row = row,
                        onAllow = { onAllow(row.permission) },
                        onOpenSettings = { onOpenSettings(row.permission) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    stringResource(
                        if (isLocationMissing) Res.string.exit_btn_txt else Res.string.permission_not_now_btn_txt,
                    ),
                color = NightInkMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(NightPillShape)
                        .clickable(role = Role.Button, onClick = if (isLocationMissing) onExit else onDismiss)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun PermissionListItem(
    row: PermissionRow,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val (icon, title, reason) =
        when (row.permission) {
            AppPermission.Location ->
                Triple(
                    Icons.Outlined.LocationOn,
                    Res.string.permission_location_title_txt,
                    Res.string.permission_location_reason_txt,
                )

            AppPermission.Notifications ->
                Triple(
                    Icons.Outlined.Notifications,
                    Res.string.permission_notification_title_txt,
                    Res.string.permission_notification_reason_txt,
                )
        }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NightInkMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(title), color = NightInk, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = stringResource(reason), color = NightInkMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        PermissionAction(status = row.status, onAllow = onAllow, onOpenSettings = onOpenSettings)
    }
}

@Composable
private fun PermissionAction(
    status: PermissionStatus,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (status) {
        PermissionStatus.NotRequested ->
            StatusPill(
                label = stringResource(Res.string.permission_allow_btn_txt),
                fill = NightInk,
                ink = NightSurface,
                onClick = onAllow,
            )

        PermissionStatus.Denied ->
            StatusPill(
                label = stringResource(Res.string.permission_denied_txt),
                fill = NightDanger.copy(alpha = 0.14f),
                ink = NightDanger,
                trailingIcon = Icons.Outlined.NorthEast,
                onClick = onOpenSettings,
            )

        PermissionStatus.Granted ->
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = NightInkMuted, modifier = Modifier.size(16.dp))
                Text(
                    text = stringResource(Res.string.permission_granted_txt),
                    color = NightInkMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
    }
}

@Composable
private fun StatusPill(
    label: String,
    fill: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier =
            Modifier
                .clip(NightPillShape)
                .background(fill)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 14.dp, end = if (trailingIcon != null) 10.dp else 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = ink, modifier = Modifier.size(14.dp))
        }
    }
}
