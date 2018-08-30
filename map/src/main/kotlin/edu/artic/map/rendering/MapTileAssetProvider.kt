package edu.artic.map.rendering

import android.content.res.AssetManager
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider

/**
 * Description: Provides map asset tiles based on the [floor] from resources.
 */
class MapTileAssetProvider(private val assetAssetManager: AssetManager,
                           private val floor: Int) : TileProvider {

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        val path = "tiles/$floor/$x/$y"
        return Tile(512, 512, assetAssetManager.open(path).readBytes())
    }
}