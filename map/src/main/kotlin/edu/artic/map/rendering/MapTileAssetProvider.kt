package edu.artic.map.rendering

import android.content.res.AssetManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.geometry.Point
import com.google.maps.android.projection.SphericalMercatorProjection
import edu.artic.map.ZOOM_MIN
import timber.log.Timber
import java.io.IOException

data class Bounds(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

private fun boundsForZoom(zoom: Int,
                          southWest: Point,
                          northEast: Point): Bounds {
    return Bounds(
            minX = tileCoordinateForWorldPosition(zoom, southWest.x),
            minY = tileCoordinateForWorldPosition(zoom, northEast.y),
            maxX = tileCoordinateForWorldPosition(zoom, northEast.x),
            maxY = tileCoordinateForWorldPosition(zoom, southWest.y))
}

private fun tileCoordinateForWorldPosition(zoom: Int, value: Double) =
        (value * (1 shl zoom).toDouble() / 512.0).toInt()

/**
 * Description: Provides map asset tiles based on the [floor] from resources.
 */
class MapTileAssetProvider(private val assetAssetManager: AssetManager,
                           private val floor: Int) : TileProvider {

    private val northEast: Point
    private val southWest: Point
    private val mapProjection = SphericalMercatorProjection(512.0)
    private val boundsMap: Map<Int, Bounds>

    init {
        val bounds = LatLngBounds(LatLng(41.874620, -87.629243),
                LatLng(41.884753, -87.615841))
        northEast = mapProjection.toPoint(bounds.northeast)
        southWest = mapProjection.toPoint(bounds.southwest)
        boundsMap = (17..22).map {
            it to boundsForZoom(
                    it,
                    northEast = northEast,
                    southWest = southWest
            )
        }.toMap()

        Timber.d("Found bounds $boundsMap")
    }


    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        val bounds = boundsMap[zoom] ?: throw IllegalStateException("Illegal zoom found $zoom")
        val adjustedZoom = (zoom - ZOOM_MIN + 2).toInt()
        val adjustedX = x - bounds.minX - 1
        val adjustedY = y - bounds.minY - 1
        Timber.d("Looking for adjusted tile from (x: $adjustedX,y: $adjustedY, z: $adjustedZoom)")
        return when {
            adjustedX >= 0 && adjustedY >= 0 -> {
                val tileXCount = Math.pow(2.toDouble(), adjustedZoom.toDouble())
                val tileNumber = (adjustedY * tileXCount + adjustedX).toInt()
                Timber.d("Tile X Count $tileXCount with number $tileNumber")

                val path = "tiles/$floor/$adjustedZoom/tiles-$tileNumber.png"
                try {
                    Tile(512, 512, assetAssetManager.open(path).readBytes()).also {
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