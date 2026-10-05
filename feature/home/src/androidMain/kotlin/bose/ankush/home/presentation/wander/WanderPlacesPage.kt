package bose.ankush.home.presentation.wander

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import bose.ankush.home.generated.resources.wander_places_active
import bose.ankush.home.generated.resources.wander_places_add
import bose.ankush.home.generated.resources.wander_places_current
import bose.ankush.home.generated.resources.wander_places_current_detail
import bose.ankush.home.generated.resources.wander_places_delete_desc
import bose.ankush.home.generated.resources.wander_places_delete_error
import bose.ankush.home.generated.resources.wander_places_empty
import bose.ankush.home.generated.resources.wander_places_load_error
import bose.ankush.home.generated.resources.wander_places_loading
import bose.ankush.home.generated.resources.wander_places_premium_body
import bose.ankush.home.generated.resources.wander_places_premium_title
import bose.ankush.home.generated.resources.wander_places_select_error
import bose.ankush.home.generated.resources.wander_places_title
import bose.ankush.home.generated.resources.wander_places_upgrade
import bose.ankush.home.presentation.places.WanderPlacesIntent
import bose.ankush.home.presentation.places.WanderPlacesLogic
import bose.ankush.home.presentation.places.WanderPlacesNotice
import org.jetbrains.compose.resources.stringResource

/** Second page of the home pager: GPS plus every saved place, as dark cards over the photo. */
@Composable
internal fun WanderPlacesPage(
    places: WanderPlacesBinding,
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
            text = stringResource(Res.string.wander_places_title),
            color = contentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(shadow = HeaderTextShadow),
        )
        state.notice?.let { notice -> PlacesNoticeLine(notice = notice, contentColor = contentColor) }
        PlaceCard(
            model =
                PlaceCardModel(
                    title = stringResource(Res.string.wander_places_current),
                    detail = stringResource(Res.string.wander_places_current_detail),
                    icon = Icons.Filled.MyLocation,
                    isActive = state.active == null,
                ),
            contentColor = contentColor,
            onClick = { onIntent(WanderPlacesIntent.UseCurrentLocation) },
        )
        SavedPlacesSection(places = places, contentColor = contentColor)
        // Keeps the last card clear of the add button.
        Spacer(modifier = Modifier.height(FabClearance))
    }
}

@Composable
private fun SavedPlacesSection(
    places: WanderPlacesBinding,
    contentColor: Color,
) {
    val state = places.state
    val onIntent = places.onIntent
    when {
        !state.isPremium -> PremiumCard(contentColor = contentColor, onUpgrade = places.onUpgrade)
        state.places.isEmpty() && state.loadFailed ->
            MessageCard(
                text = stringResource(Res.string.wander_places_load_error),
                contentColor = contentColor,
                action = stringResource(Res.string.retry_btn_txt) to { onIntent(WanderPlacesIntent.Load) },
            )
        state.places.isEmpty() && state.isLoading ->
            Text(
                text = stringResource(Res.string.wander_places_loading),
                color = contentColor.copy(alpha = MUTED_ALPHA),
                fontSize = 13.sp,
            )
        state.places.isEmpty() ->
            MessageCard(text = stringResource(Res.string.wander_places_empty), contentColor = contentColor)
        else ->
            state.places.forEach { place ->
                val deleteLabel = stringResource(Res.string.wander_places_delete_desc, place.name)
                PlaceCard(
                    model =
                        PlaceCardModel(
                            title = place.name,
                            detail = WanderPlacesLogic.formatCoordinates(place.lat, place.lon),
                            icon = Icons.Filled.LocationOn,
                            isActive = WanderPlacesLogic.isActive(place, state.active),
                            deleteLabel = deleteLabel.takeIf { place.id.isNotBlank() },
                        ),
                    contentColor = contentColor,
                    onClick = { onIntent(WanderPlacesIntent.Select(place)) },
                    onDelete = { onIntent(WanderPlacesIntent.Delete(place.id)) },
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
        if (model.isActive) ActiveBadge()
        val deleteLabel = model.deleteLabel
        if (deleteLabel != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = deleteLabel,
                    tint = contentColor.copy(alpha = MUTED_ALPHA),
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
private fun ActiveBadge() {
    Text(
        text = stringResource(Res.string.wander_places_active),
        color = ToastOnWarning,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier =
            Modifier
                .background(WarningYellow, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

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
            text = stringResource(Res.string.wander_places_premium_title),
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(Res.string.wander_places_premium_body),
            color = contentColor.copy(alpha = MUTED_ALPHA),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.wander_places_upgrade),
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
            WanderActionLabel(label = label, onClick = onClick, contentColor = contentColor)
        }
    }
}

@Composable
private fun PlacesNoticeLine(
    notice: WanderPlacesNotice,
    contentColor: Color,
) {
    val text =
        when (notice) {
            WanderPlacesNotice.DeleteFailed -> stringResource(Res.string.wander_places_delete_error)
            WanderPlacesNotice.SelectFailed -> stringResource(Res.string.wander_places_select_error)
        }
    Text(text = text, color = contentColor.copy(alpha = NOTICE_ALPHA), fontSize = 13.sp)
}

/** Add-a-place button. Shown only while the places page is the current page. */
@Composable
internal fun WanderAddPlaceButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = WarningYellow,
            contentColor = ToastOnWarning,
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(Res.string.wander_places_add))
        }
    }
}

private val PlaceCardFill = Color.Black.copy(alpha = 0.38f)
private val IconDiscFill = Color.White.copy(alpha = 0.12f)
private val ActiveOutline = WarningYellow.copy(alpha = 0.8f)
private val FabClearance = 72.dp
private const val MUTED_ALPHA = 0.72f
private const val NOTICE_ALPHA = 0.9f
