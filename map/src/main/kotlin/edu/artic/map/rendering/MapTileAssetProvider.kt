package edu.artic.map.rendering

import android.content.res.AssetManager
import com.google.android.gms.maps.model.Tile
import timber.log.Timber
import java.io.IOException
import kotlin.math.pow

/**
 * Description: Provides map asset tiles based on the [floor] from resources.
 */
open class MapTileAssetProvider(private val assetAssetManager: AssetManager,
                                floor: Int) : BaseMapTileProvider(floor) {

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