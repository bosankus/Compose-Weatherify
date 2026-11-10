package bose.ankush.home.presentation.places

import bose.ankush.home.domain.nearby.GeoPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/** One slippy-map tile and where its top-left corner lands in the viewport, in tile units (dp). */
internal data class MapTile(
    val x: Int,
    val y: Int,
    val zoom: Int,
    val left: Double,
    val top: Double,
)

/**
 * Web Mercator maths for the places page map backdrop. A viewport is [width] by [height]
 * (in the same units as [TILE_SIZE]) with [center] drawn at ([width] / 2, [height] * [focusY]).
 */
internal data class PlacesMapViewport(
    val center: GeoPoint,
    val zoom: Int,
    val width: Double,
    val height: Double,
    val focusY: Double = DEFAULT_FOCUS_Y,
) {
    private val centerX = worldX(center.lon, zoom)
    private val centerY = worldY(center.lat, zoom)
    private val originX = centerX - width / 2
    private val originY = centerY - height * focusY

    /** Every tile that touches the viewport. Columns wrap at the antimeridian; rows off the poles are skipped. */
    fun tiles(): List<MapTile> {
        val tileCount = 1 shl zoom
        val firstColumn = floor(originX / TILE_SIZE).toInt()
        val lastColumn = floor((originX + width) / TILE_SIZE).toInt()
        val firstRow = floor(originY / TILE_SIZE).toInt().coerceAtLeast(0)
        val lastRow = floor((originY + height) / TILE_SIZE).toInt().coerceAtMost(tileCount - 1)
        return buildList {
            for (row in firstRow..lastRow) {
                for (column in firstColumn..lastColumn) {
                    add(
                        MapTile(
                            x = column.mod(tileCount),
                            y = row,
                            zoom = zoom,
                            left = column * TILE_SIZE - originX,
                            top = row * TILE_SIZE - originY,
                        ),
                    )
                }
            }
        }
    }

    /** Where [point] lands in the viewport, or null when it falls outside it (plus [margin]). */
    fun positionOf(
        point: GeoPoint,
        margin: Double = 0.0,
    ): Pair<Double, Double>? {
        val worldSize = TILE_SIZE * (1 shl zoom)
        var x = worldX(point.lon, zoom) - originX
        // Take the copy of the world nearest the viewport, so places across the antimeridian still show.
        if (x < -worldSize / 2) x += worldSize
        if (x > worldSize / 2) x -= worldSize
        val y = worldY(point.lat, zoom) - originY
        val inside = x in -margin..(width + margin) && y in -margin..(height + margin)
        return if (inside) x to y else null
    }

    companion object {
        const val TILE_SIZE = 256.0
        const val DEFAULT_FOCUS_Y = 0.3

        /** Mercator stops here; beyond it the projection runs off to infinity. */
        private const val MAX_LATITUDE = 85.05112878

        fun worldX(
            lon: Double,
            zoom: Int,
        ): Double = (lon + 180.0) / 360.0 * TILE_SIZE * 2.0.pow(zoom)

        fun worldY(
            lat: Double,
            zoom: Int,
        ): Double {
            val radians = lat.coerceIn(-MAX_LATITUDE, MAX_LATITUDE) * PI / 180.0
            val mercator = ln(tan(radians) + 1 / cos(radians))
            return (1 - mercator / PI) / 2 * TILE_SIZE * 2.0.pow(zoom)
        }
    }
}
