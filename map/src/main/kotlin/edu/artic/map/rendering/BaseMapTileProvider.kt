package edu.artic.map.rendering

import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.geometry.Point
import com.google.maps.android.projection.SphericalMercatorProjection
import edu.artic.location.museumBounds
import edu.artic.map.ZOOM_MIN
import timber.log.Timber

data class Bounds(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

internal fun boundsForZoom(zoom: Int,
                           southWest: Point,
                           northEast: Point): Bounds {
    return Bounds(
            minX = tileCoordinateForWorldPosition(zoom, southWest.x),
            minY = tileCoordinateForWorldPosition(zoom, northEast.y),
            maxX = tileCoordinateForWorldPosition(zoom, northEast.x),
            maxY = tileCoordinateForWorldPosition(zoom, southWest.y))
}

internal const val TILE_SIZE = 512.0

private fun tileCoordinateForWorldPosition(zoom: Int, value: Double): Int {
    val d = value * (1 shl zoom).toDouble() / TILE_SIZE
    Timber.d("Found tile coordinate approx $d for zoom $zoom")
    return d.toInt()
}

internal val evaluatedBoundsMap
    get() = mapOf(
            17 to Bounds(minX = 33631, minY = 48713, maxX = 33635, maxY = 48716),
            18 to Bounds(minX = 67263, minY = 97426, maxX = 67270, maxY = 97433),
            19 to Bounds(minX = 134527, minY = 194852, maxX = 134541, maxY = 194868),
            20 to Bounds(minX = 269055, minY = 389704, maxX = 269084, maxY = 389733),
            21 to Bounds(minX = 538108, minY = 779412, maxX = 538167, maxY = 779471),
            22 to Bounds(minX = 1076217, minY = 1558824, maxX = 1076334, maxY = 1558942))

abstract class BaseMapTileProvider : TileProvider {

    protected val northEast: Point
    protected val southWest: Point
    protected val mapProjection = SphericalMercatorProjection(TILE_SIZE)
    protected val boundsMap: Map<Int, Bounds>

    init {
        northEast = mapProjection.toPoint(museumBounds.northeast)
        southWest = mapProjection.toPoint(museumBounds.southwest)

        // use this snippet of code to calculate bounds map for you.
        /*boundsMap = (ZOOM_MIN.toInt()..ZOOM_MAX.toInt()).map {
            it to boundsForZoom(
                    it,
                    northEast = northEast,
                    southWest = southWest
            )
        }.toMap()*/
        boundsMap = evaluatedBoundsMap
        Timber.d("Found bounds $boundsMap")
    }


    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        val bounds = boundsMap[zoom] ?: throw IllegalStateException("Illegal zoom found $zoom")
        val tileLevel = (zoom - ZOOM_MIN + 2).toInt()
        val adjustedX = if(x >=  bounds.minX && x <= bounds.maxX) x - bounds.minX - 1 else -1
        val adjustedY = if(y >= bounds.minY && y <= bounds.maxY) y - bounds.minY else -1

        Timber.d("GetTile x: $x y: $y adjustedX : $adjustedX adjustedY: $adjustedY")

        return getAdjustedTileWrapper(x, y, zoom, adjustedX, adjustedY, tileLevel)
    }

    protected open fun getAdjustedTileWrapper(
            originalX: Int,
            originalY: Int,
            originalZoom: Int,
            x: Int,
            y: Int,
            zoom: Int): Tile? {
        return getAdjustedTile(x, y, zoom)
    }

    abstract fun getAdjustedTile(x: Int, y: Int, zoom: Int): Tile?
}
