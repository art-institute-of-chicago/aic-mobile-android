package edu.artic.map.rendering

import android.content.res.AssetManager
import com.google.android.gms.maps.model.Tile
import com.google.maps.android.geometry.Point
import com.google.maps.android.projection.SphericalMercatorProjection
import edu.artic.map.museumBounds
import timber.log.Timber
import java.io.IOException
import kotlin.math.pow

/**
 * Description: Provides map asset tiles based on the [floor] from resources.
 */
open class MapTileAssetProvider(private val assetAssetManager: AssetManager,
                                floor: Int) : BaseMapTileProvider(floor) {

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

    override fun getAdjustedTile(x: Int, y: Int, zoom: Int): Tile? {
        return when {
            x >= 0 && y >= 0 -> {
                // calculate tile row count
                val tileXCount = (2.0).pow(zoom)

                // grab which tile based on row, what y number it is in the tile list, x position
                val tileNumber = (y * tileXCount + x).toInt()
                Timber.d("Tile X Count $tileXCount with number $tileNumber")

                val path = "tiles/$floor/$zoom/tiles-$tileNumber.png"
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