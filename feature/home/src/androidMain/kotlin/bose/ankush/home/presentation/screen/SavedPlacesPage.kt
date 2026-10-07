package bose.ankush.home.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.ToastOnWarning
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.retry_btn_txt
import bose.ankush.home.generated.resources.saved_places_add
import bose.ankush.home.generated.resources.saved_places_current
import bose.ankush.home.generated.resources.saved_places_current_detail
import bose.ankush.home.generated.resources.saved_places_delete_desc
import bose.ankush.home.generated.resources.saved_places_delete_error
import bose.ankush.home.generated.resources.saved_places_empty
import bose.ankush.home.generated.resources.saved_places_load_error
import bose.ankush.home.generated.resources.saved_places_loading
import bose.ankush.home.generated.resources.saved_places_premium_body
import bose.ankush.home.generated.resources.saved_places_premium_title
import bose.ankush.home.generated.resources.saved_places_select_error
import bose.ankush.home.generated.resources.saved_places_title
import bose.ankush.home.generated.resources.saved_places_upgrade
import bose.ankush.home.presentation.places.SavedPlacesIntent
import bose.ankush.home.presentation.places.SavedPlacesLogic
import bose.ankush.home.presentation.places.SavedPlacesNotice
import org.jetbrains.compose.resources.stringResource

/** Second page of the home pager: GPS plus every saved place, as dark cards over the photo. */
@Composable
internal fun SavedPlacesPage(
    places: SavedPlacesBinding,
    contentColor: Color,
    minHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val state = places.state
    val onIntent = places.onIntent
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.saved_places_title),
            color = contentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(shadow = HeaderTextShadow),
        )
        state.notice?.let { notice -> PlacesNoticeLine(notice = notice, contentColor = contentColor) }
        PlaceCard(
            model =
                PlaceCardModel(
                    title = stringResource(Res.string.saved_places_current),
                    detail = stringResource(Res.string.saved_places_current_detail),
                    icon = Icons.Filled.MyLocation,
                    isActive = state.active == null,
                ),
            contentColor = contentColor,
            onClick = { onIntent(SavedPlacesIntent.UseCurrentLocation) },
        )
        SavedPlacesSection(places = places, contentColor = contentColor)
        // Keeps the last card clear of the add button.
        Spacer(modifier = Modifier.height(FabClearance))
    }
}

@Composable
private fun SavedPlacesSection(
    places: SavedPlacesBinding,
    contentColor: Color,
) {
    val state = places.state
    val onIntent = places.onIntent
    when {
        !state.isPremium -> PremiumCard(contentColor = contentColor, onUpgrade = places.onUpgrade)
        state.places.isEmpty() && state.loadFailed ->
            MessageCard(
                text = stringResource(Res.string.saved_places_load_error),
                contentColor = contentColor,
                action = stringResource(Res.string.retry_btn_txt) to { onIntent(SavedPlacesIntent.Load) },
            )
        state.places.isEmpty() && state.isLoading ->
            Text(
                text = stringResource(Res.string.saved_places_loading),
                color = contentColor.copy(alpha = MUTED_ALPHA),
                fontSize = 13.sp,
            )
        state.places.isEmpty() ->
            MessageCard(
                text = stringResource(Res.string.saved_places_empty),
                contentColor = contentColor
            )
        else ->
            state.places.forEach { place ->
                val deleteLabel = stringResource(Res.string.saved_places_delete_desc, place.name)
                PlaceCard(
                    model =
                        PlaceCardModel(
                            title = place.name,
                            detail = SavedPlacesLogic.formatCoordinates(place.lat, place.lon),
                            icon = Icons.Filled.LocationOn,
                            isActive = SavedPlacesLogic.isActive(place, state.active),
                            deleteLabel = deleteLabel.takeIf { place.id.isNotBlank() },
                        ),
                    contentColor = contentColor,
                    onClick = { onIntent(SavedPlacesIntent.Select(place)) },
                    onDelete = { onIntent(SavedPlacesIntent.Delete(place.id)) },
                )
            }
    }
}

private data class PlaceCardModel(
    val title: String,
    val detail: String,
    val icon: ImageVector,
    val isActive: Boolean,
    /** Null hides the remove button (GPS, or a place still being stored). */
    val deleteLabel: String? = null,
)

@Composable
private fun PlaceCard(
    model: PlaceCardModel,
    contentColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(PlaceCardFill)
                .then(if (model.isActive) Modifier.border(1.dp, ActiveOutline, shape) else Modifier)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    selected = model.isActive
                }.clickable(onClick = onClick)
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(IconDiscFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = model.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(
                text = model.title,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = model.detail,
                color = contentColor.copy(alpha = MUTED_ALPHA),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PlaceCardEnd(
            trailing = placeCardTrailing(isActive = model.isActive, canDelete = model.deleteLabel != null),
            deleteLabel = model.deleteLabel.orEmpty(),
            contentColor = contentColor,
            onDelete = onDelete,
        )
    }
}

/**
 * Right end of a place card: a tick on the place Home is showing, otherwise the remove
 * button (or nothing). Switching between them crossfades with a small scale.
 */
@Composable
private fun PlaceCardEnd(
    trailing: PlaceCardTrailing,
    deleteLabel: String,
    contentColor: Color,
    onDelete: () -> Unit,
) {
    AnimatedContent(
        targetState = trailing,
        transitionSpec = {
            (fadeIn(trailingTween()) + scaleIn(trailingTween(), initialScale = TRAILING_START_SCALE))
                .togetherWith(fadeOut(trailingTween()) + scaleOut(trailingTween(), targetScale = TRAILING_START_SCALE))
        },
        contentAlignment = Alignment.Center,
        label = "placeCardTrailing",
    ) { state ->
        when (state) {
            PlaceCardTrailing.Selected -> SelectedTick()
            PlaceCardTrailing.Remove ->
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = deleteLabel,
                        tint = contentColor.copy(alpha = MUTED_ALPHA),
                        modifier = Modifier.size(18.dp),
                    )
                }

            PlaceCardTrailing.None -> Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
private fun SelectedTick() {
    // Same 48dp slot as the remove button, so the text column never jumps.
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(WarningYellow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = ToastOnWarning,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun <T> trailingTween() = tween<T>(durationMillis = TRAILING_ANIM_MILLIS)

private fun <T> fabTween() = tween<T>(durationMillis = FAB_ANIM_MILLIS)

@Composable
private fun PremiumCard(
    contentColor: Color,
    onUpgrade: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(PlaceCardFill, RoundedCornerShape(20.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.saved_places_premium_title),
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(Res.string.saved_places_premium_body),
            color = contentColor.copy(alpha = MUTED_ALPHA),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.saved_places_upgrade),
            color = ToastOnWarning,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(WarningYellow)
                    .clickable(role = Role.Button, onClick = onUpgrade)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MessageCard(
    text: String,
    contentColor: Color,
    action: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(PlaceCardFill, RoundedCornerShape(20.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = text, color = contentColor, fontSize = 14.sp)
        action?.let { (label, onClick) ->
            ActionLabel(label = label, onClick = onClick, contentColor = contentColor)
        }
    }
}

@Composable
private fun PlacesNoticeLine(
    notice: SavedPlacesNotice,
    contentColor: Color,
) {
    val text =
        when (notice) {
            SavedPlacesNotice.DeleteFailed -> stringResource(Res.string.saved_places_delete_error)
            SavedPlacesNotice.SelectFailed -> stringResource(Res.string.saved_places_select_error)
        }
    Text(text = text, color = contentColor.copy(alpha = NOTICE_ALPHA), fontSize = 13.sp)
}

/**
 * Add-a-place button. Shown only on the places page, and hidden while the add sheet is
 * open; it shrinks and fades out as the sheet rises, and comes back when it closes.
 */
@Composable
internal fun AddPlaceButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(fabTween(), initialScale = FAB_HIDDEN_SCALE) + fadeIn(fabTween()),
        exit = scaleOut(fabTween(), targetScale = FAB_HIDDEN_SCALE) + fadeOut(fabTween()),
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = WarningYellow,
            contentColor = ToastOnWarning,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(Res.string.saved_places_add)
            )
        }
    }
}

private val PlaceCardFill = Color.Black.copy(alpha = 0.38f)
private val IconDiscFill = Color.White.copy(alpha = 0.12f)
private val ActiveOutline = WarningYellow.copy(alpha = 0.8f)
private val FabClearance = 72.dp
private const val MUTED_ALPHA = 0.72f
private const val NOTICE_ALPHA = 0.9f
private const val TRAILING_ANIM_MILLIS = 200
private const val TRAILING_START_SCALE = 0.6f
private const val FAB_ANIM_MILLIS = 200
private const val FAB_HIDDEN_SCALE = 0.4f
