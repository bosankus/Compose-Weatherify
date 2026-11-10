package bose.ankush.home.presentation.places

/**
 * Esri's World Dark Gray Canvas, the street map behind the places page on Android and iOS.
 * The tile licence asks for [ATTRIBUTION] wherever the map shows.
 */
internal object PlacesMapTiles {
    const val BASE_URL =
        "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile"

    /** Esri's tile path is zoom/row/column, the reverse of the usual x/y order. */
    const val URL_TEMPLATE = "$BASE_URL/{z}/{y}/{x}"

    const val ATTRIBUTION = "Esri, HERE, Garmin, © OpenStreetMap contributors"

    /** Zoom the map is shown at, in 256-unit tiles. */
    const val ZOOM = 11
}
