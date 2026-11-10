package bose.ankush.home.presentation.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.finder.domain.model.Location
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.presentation.places.MapTile
import bose.ankush.home.presentation.places.PlacesMapTiles
import bose.ankush.home.presentation.places.PlacesMapViewport
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Dark street map behind the places page, centred on the place Home is showing.
 *
 * Loads gracefully: a flat map-coloured fill shows at once, then the whole map fades in
 * and settles from a slight zoom once most tiles have arrived (or after a short wait, so a
 * slow network still gets a map). Late tiles simply appear. Moving to another place
 * crossfades to the new map. Saved places within view get a pin; the shown place pulses
 * while [visible].
 *
 * The reveal is one animation on one layer, not one per tile, so it touches only the draw
 * phase of a single node. Tiles are Esri's World Dark Gray Canvas, so the page must credit
 * Esri and its sources.
 *
 * Esri serves 256px tiles. To stay sharp on dense screens the map asks for one zoom level
 * deeper than it shows and draws each tile at half size ([TILE_DRAW_SCALE]).
 */
@Composable
internal fun PlacesMapBackdrop(
    center: GeoPoint?,
    places: List<Location>,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MapBaseFill)) {
        Crossfade(
            targetState = center,
            animationSpec = tween(durationMillis = MAP_SWAP_MILLIS),
            label = "placesMapCenter",
        ) { focus ->
            if (focus != null) PlacesMap(center = focus, places = places, visible = visible)
        }
        Box(modifier = Modifier.fillMaxSize().background(MapReadabilityScrim))
    }
}

@Composable
private fun PlacesMap(
    center: GeoPoint,
    places: List<Location>,
    visible: Boolean,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewport =
            remember(center, maxWidth, maxHeight) {
                PlacesMapViewport(
                    center = center,
                    zoom = MAP_ZOOM + 1,
                    width = maxWidth.value / TILE_DRAW_SCALE,
                    height = maxHeight.value / TILE_DRAW_SCALE,
                )
            }
        val tiles = remember(viewport) { viewport.tiles() }
        val reveal = remember(viewport) { Animatable(0f) }
        // Written from Coil callbacks and read only by the effect below, never by composition,
        // so counting tiles never recomposes anything.
        val loadedTiles = remember(viewport) { mutableIntStateOf(0) }
        LaunchedEffect(reveal) {
            val ready = (tiles.size * REVEAL_TILE_SHARE).toInt()
            withTimeoutOrNull(REVEAL_TIMEOUT_MILLIS) {
                snapshotFlow { loadedTiles.intValue }.first { it >= ready }
            }
            reveal.animateTo(
                1f,
                tween(durationMillis = MAP_SETTLE_MILLIS, easing = FastOutSlowInEasing)
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val progress = reveal.value
                        alpha = progress
                        scaleX = MAP_START_SCALE + (1f - MAP_START_SCALE) * progress
                        scaleY = scaleX
                        // Settle towards the shown place, not the middle of the screen.
                        transformOrigin =
                            TransformOrigin(0.5f, PlacesMapViewport.DEFAULT_FOCUS_Y.toFloat())
                    },
        ) {
            tiles.forEach { tile ->
                MapTileImage(
                    tile = tile,
                    onLoaded = { loadedTiles.intValue++ })
            }

            places.forEach { place ->
                val point = GeoPoint(place.lat, place.lon)
                if (point == center) return@forEach
                viewport.positionOf(point, margin = PIN_SIZE.value / TILE_DRAW_SCALE)
                    ?.let { (x, y) ->
                        SavedPlacePin(x = (x * TILE_DRAW_SCALE).dp, y = (y * TILE_DRAW_SCALE).dp)
                    }
            }
            viewport.positionOf(center)?.let { (x, y) ->
                ShownPlacePin(
                    x = (x * TILE_DRAW_SCALE).dp,
                    y = (y * TILE_DRAW_SCALE).dp,
                    pulsing = visible
                )
            }
        }
    }
}

@Composable
private fun MapTileImage(
    tile: MapTile,
    onLoaded: () -> Unit,
) {
    val context = LocalContext.current
    val request =
        remember(tile, context) {
            ImageRequest
                .Builder(context)
                .data(tileUrl(tile))
                .build()
        }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        onSuccess = { onLoaded() },
        modifier =
            Modifier
                .offset(x = (tile.left * TILE_DRAW_SCALE).dp, y = (tile.top * TILE_DRAW_SCALE).dp)
                .size((PlacesMapViewport.TILE_SIZE * TILE_DRAW_SCALE).dp),
    )
}

@Composable
private fun SavedPlacePin(
    x: Dp,
    y: Dp,
) {
    Box(
        modifier =
            Modifier
                .offset(x = x - PIN_SIZE / 2, y = y - PIN_SIZE / 2)
                .size(PIN_SIZE)
                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .background(WarningYellow.copy(alpha = 0.85f), CircleShape),
    )
}

@Composable
private fun ShownPlacePin(
    x: Dp,
    y: Dp,
    pulsing: Boolean,
) {
    // The infinite transition exists only while pulsing, so a hidden map asks for no frames.
    if (pulsing) PulseRing(x = x, y = y)
    Box(
        modifier =
            Modifier
                .offset(x = x - SHOWN_PIN_SIZE / 2, y = y - SHOWN_PIN_SIZE / 2)
                .size(SHOWN_PIN_SIZE)
                .border(3.dp, Color.White, CircleShape)
                .background(WarningYellow, CircleShape),
    )
}

@Composable
private fun PulseRing(
    x: Dp,
    y: Dp,
) {
    val pulse = rememberInfiniteTransition(label = "shownPlacePulse")
    val progress = pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(PULSE_MILLIS, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "shownPlacePulseProgress",
    )
    Box(
        modifier =
            Modifier
                .offset(x = x - PULSE_SIZE / 2, y = y - PULSE_SIZE / 2)
                .size(PULSE_SIZE)
                .graphicsLayer {
                    // Read here, not in composition, so each pulse frame is a redraw only.
                    scaleX = PULSE_MIN_SCALE + (1f - PULSE_MIN_SCALE) * progress.value
                    scaleY = scaleX
                    alpha = 1f - progress.value
                }.background(WarningYellow.copy(alpha = 0.45f), CircleShape),
    )
}

/** Esri's tile path is zoom/row/column, the reverse of the usual x/y order. */
private fun tileUrl(tile: MapTile): String =
    "${PlacesMapTiles.BASE_URL}/${tile.zoom}/${tile.y}/${tile.x}"

/** Credit line the tile licence asks for wherever the map shows. */
internal const val PLACES_MAP_ATTRIBUTION = PlacesMapTiles.ATTRIBUTION

/** Each 256px tile covers 128dp, so a 2.5x screen gets nearly one source pixel per device pixel. */
private const val TILE_DRAW_SCALE = 0.5

/** Dark Gray Canvas's own land colour, so the gaps before tiles arrive don't flash. */
private val MapBaseFill = Color(0xFF242428)
private val MapReadabilityScrim =
    Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.25f),
        0.45f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.55f),
    )
private val PIN_SIZE = 14.dp
private val SHOWN_PIN_SIZE = 18.dp
private val PULSE_SIZE = 56.dp
private const val PULSE_MIN_SCALE = 0.3f
private const val PULSE_MILLIS = 2_000
private const val MAP_ZOOM = PlacesMapTiles.ZOOM
private const val MAP_START_SCALE = 1.08f
private const val MAP_SETTLE_MILLIS = 1_400
private const val MAP_SWAP_MILLIS = 600

/** Reveal once this share of tiles is in; the rest appear as they arrive. */
private const val REVEAL_TILE_SHARE = 0.8
private const val REVEAL_TIMEOUT_MILLIS = 1_500L
