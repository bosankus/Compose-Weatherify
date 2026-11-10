package bose.ankush.home.presentation.places

import bose.ankush.home.domain.nearby.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlacesMapViewportTest {
    @Test
    fun worldOriginIsTopLeftAndCenterIsHalfway() {
        assertEquals(0.0, PlacesMapViewport.worldX(-180.0, 0), DELTA)
        assertEquals(128.0, PlacesMapViewport.worldX(0.0, 0), DELTA)
        assertEquals(128.0, PlacesMapViewport.worldY(0.0, 0), DELTA)
        assertEquals(256.0, PlacesMapViewport.worldX(0.0, 1), DELTA)
    }

    @Test
    fun centerLandsAtTheFocusPoint() {
        val viewport = PlacesMapViewport(
            center = PARIS,
            zoom = 11,
            width = 400.0,
            height = 800.0,
            focusY = 0.3
        )
        val (x, y) = assertNotNull(viewport.positionOf(PARIS))
        assertEquals(200.0, x, DELTA)
        assertEquals(240.0, y, DELTA)
    }

    @Test
    fun tilesCoverTheWholeViewport() {
        val viewport = PlacesMapViewport(center = PARIS, zoom = 11, width = 400.0, height = 800.0)
        val tiles = viewport.tiles()
        assertTrue(tiles.all { it.zoom == 11 })
        assertTrue(tiles.minOf { it.left } <= 0.0)
        assertTrue(tiles.minOf { it.top } <= 0.0)
        assertTrue(tiles.maxOf { it.left } + PlacesMapViewport.TILE_SIZE >= 400.0)
        assertTrue(tiles.maxOf { it.top } + PlacesMapViewport.TILE_SIZE >= 800.0)
        // Paris at zoom 11 sits in tile column 1037, row 704.
        assertTrue(tiles.any { it.x == 1037 && it.y == 704 })
    }

    @Test
    fun columnsWrapAtTheAntimeridian() {
        val viewport = PlacesMapViewport(
            center = GeoPoint(lat = 0.0, lon = 179.99),
            zoom = 2,
            width = 512.0,
            height = 256.0
        )
        val columns = viewport.tiles().map { it.x }.toSet()
        assertTrue(columns.all { it in 0..3 })
        assertTrue(0 in columns && 3 in columns)
    }

    @Test
    fun rowsBeyondThePolesAreSkipped() {
        val viewport = PlacesMapViewport(
            center = GeoPoint(lat = 84.0, lon = 0.0),
            zoom = 1,
            width = 256.0,
            height = 1000.0
        )
        assertTrue(viewport.tiles().all { it.y in 0..1 })
    }

    @Test
    fun farPlacesAreOffscreen() {
        val viewport = PlacesMapViewport(center = PARIS, zoom = 11, width = 400.0, height = 800.0)
        assertNull(viewport.positionOf(GeoPoint(lat = 40.7128, lon = -74.0060)))
        assertNotNull(viewport.positionOf(GeoPoint(lat = 48.86, lon = 2.36)))
    }

    private companion object {
        val PARIS = GeoPoint(lat = 48.8566, lon = 2.3522)
        const val DELTA = 1e-6
    }
}
