package bose.ankush.settings.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.photo.rememberAccountPhotoRequest
import bose.ankush.commonui.theme.ToastOnWarning
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.settings.generated.resources.Res
import bose.ankush.settings.generated.resources.default_avatar
import bose.ankush.settings.generated.resources.profile_add_photo_txt
import bose.ankush.settings.generated.resources.profile_edit_photo_content_desc
import bose.ankush.settings.generated.resources.profile_photo_content_desc
import bose.ankush.settings.presentation.profile.ProfileState
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What the header can do. [onChangePhoto] opens the picker; it is null where the platform cannot
 * pick photos, which hides the badge and leaves a read-only avatar. [onOpenPhotoOptions] opens
 * the change-or-remove sheet once there is a photo.
 */
internal class ProfileHeaderActions(
    val onChangePhoto: (() -> Unit)?,
    val onOpenPhotoOptions: () -> Unit,
)

@Composable
internal fun ProfileHeader(
    state: ProfileState,
    actions: ProfileHeaderActions,
    modifier: Modifier = Modifier,
) {
    // No photo yet: straight to the picker. A photo: the sheet offers change or remove.
    val onAvatarClick =
        actions.onChangePhoto?.let { pick -> if (state.photoUrl == null) pick else actions.onOpenPhotoOptions }
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EditableAvatar(
            photoUrl = state.photoUrl,
            isBusy = state.isBusy,
            onClick = onAvatarClick?.takeUnless { state.isBusy },
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            // Keeps the line's height while the account loads, so nothing below jumps.
            text = state.email ?: " ",
            color = SettingsInk,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EditableAvatar(
    photoUrl: String?,
    isBusy: Boolean,
    onClick: (() -> Unit)?,
) {
    val description = stringResource(Res.string.profile_photo_content_desc)
    val editDescription =
        stringResource(
            if (photoUrl == null) Res.string.profile_add_photo_txt else Res.string.profile_edit_photo_content_desc,
        )
    Box(modifier = Modifier.size(AvatarSize)) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(SettingsCardFill)
                    .border(2.dp, SettingsInkFaint, CircleShape)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                onClickLabel = editDescription,
                                role = Role.Button,
                                onClick = onClick
                            )
                        } else {
                            Modifier
                        },
                    ),
        ) {
            AvatarImage(photoUrl = photoUrl, contentDescription = description)
            if (isBusy) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = BUSY_SCRIM_ALPHA)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = SettingsInk,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        if (onClick != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(BadgeSize)
                        .clip(CircleShape)
                        .background(WarningYellow)
                        .border(3.dp, BadgeRing, CircleShape)
                        .clickable(
                            onClickLabel = editDescription,
                            role = Role.Button,
                            onClick = onClick
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (photoUrl == null) Icons.Outlined.PhotoCamera else Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = ToastOnWarning,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Cached by the photo's path, so the re-signed URL each refresh brings is not a new download. */
@Composable
private fun AvatarImage(
    photoUrl: String?,
    contentDescription: String,
) {
    val fallback = painterResource(Res.drawable.default_avatar)
    if (photoUrl == null) {
        Image(
            painter = fallback,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        AsyncImage(
            model = rememberAccountPhotoRequest(photoUrl),
            placeholder = fallback,
            error = fallback,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal enum class PillStyle(
    val content: Color,
    val container: Color,
) {
    Neutral(SettingsInk, SettingsInkFaint),
    Danger(SettingsDanger, SettingsInkFaint),
    Accent(ToastOnWarning, WarningYellow),
}

@Composable
internal fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: PillStyle = PillStyle.Neutral,
) {
    Box(
        modifier =
            modifier
                .clip(SettingsPillShape)
                .background(style.container)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) style.content else style.content.copy(alpha = DISABLED_ALPHA),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private val AvatarSize = 112.dp
private val BadgeSize = 34.dp
private val BadgeRing = Color(0xFF161B22)
private const val BUSY_SCRIM_ALPHA = 0.45f
private const val DISABLED_ALPHA = 0.4f
