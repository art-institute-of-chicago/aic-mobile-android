package edu.artic.map.rendering

import android.content.res.AssetManager
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import timber.log.Timber
import java.io.IOException

/**
 * Description:
 */
class MapTileAssetProvider2(private val assetManager: AssetManager,
                            private val floor: Int) : TileProvider {
    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        val tileXCount = Math.pow(2.toDouble(), zoom.toDouble())
        val tileNumber = (y * tileXCount + x).toInt()
        Timber.d("Tile X Count $tileXCount with number $tileNumber")
        val path = "tiles/$floor/$zoom/tiles-$tileNumber.png"
        return try {
            Tile(512, 512, assetManager.open(path).readBytes()).also {
                Timber.d("Loaded tile with path $path")
            }
        } catch (e: IOException) {
            Timber.e("Could not find tile with $path")
            null
        }
    }
}