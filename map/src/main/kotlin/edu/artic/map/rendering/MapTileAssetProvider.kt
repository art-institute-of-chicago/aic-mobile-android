package edu.artic.map.rendering

import android.content.res.AssetManager
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.geometry.Point
import com.google.maps.android.projection.SphericalMercatorProjection
import edu.artic.map.ZOOM_MIN
import edu.artic.map.museumBounds
import timber.log.Timber
import java.io.IOException
import kotlin.math.pow

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

internal val TILE_SIZE = 256.0

private fun tileCoordinateForWorldPosition(zoom: Int, value: Double): Int {
    val d = value * (1 shl zoom).toDouble() / TILE_SIZE
    Timber.d("Found tile coordinate approx $d for zoom $zoom")
    return d.toInt()
}

internal val evaluatedBoundsMap
    get() = mapOf(
            17 to Bounds(minX = 33631, minY = 48713, maxX = 33635, maxY = 48716),
            18 to Bounds(minX = 67263, minY = 97426, maxX = 67270, maxY = 97433),
            19 to Bounds(minX = 134527, minY = 194852, maxX = 134541, maxY = 194867),
            20 to Bounds(minX = 269054, minY = 389706, maxX = 269083, maxY = 389735),
            21 to Bounds(minX = 538108, minY = 779412, maxX = 538167, maxY = 779471),
            22 to Bounds(minX = 1076217, minY = 1558824, maxX = 1076334, maxY = 1558942))

/**
 * Description: Provides map asset tiles based on the [floor] from resources.
 */
class MapTileAssetProvider(private val assetAssetManager: AssetManager,
                           private val floor: Int) : TileProvider {

    private val northEast: Point
    private val southWest: Point
    private val mapProjection = SphericalMercatorProjection(TILE_SIZE)
    private val boundsMap: Map<Int, Bounds>

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
        val adjustedX = x - bounds.minX - 1
        val adjustedY = y - bounds.minY
        Timber.d("Looking for adjusted tile from (x: $adjustedX,y: $adjustedY, z: $tileLevel)")
        return when {
            adjustedX >= 0 && adjustedY >= 0 -> {
                // calculate tile row count
                val tileXCount = (2.0).pow(tileLevel)

                // grab which tile based on row, what y number it is in the tile list, x position
                val tileNumber = (adjustedY * tileXCount + adjustedX).toInt()
                Timber.d("Tile X Count $tileXCount with number $tileNumber")

                val path = "tiles/$floor/$tileLevel/tiles-$tileNumber.png"
                try {
                    Tile(TILE_SIZE.toInt(), TILE_SIZE.toInt(), assetAssetManager.open(path).readBytes()).also {
                        Timber.d("Loaded tile with path $path")
                    }
                } catch (e: IOException) {
                    Timber.e("Could not find tile with $path")
                    return null
                }
            }
            else -> null
        }
    }
}