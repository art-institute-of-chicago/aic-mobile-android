package edu.artic.map.rendering

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.Tile
import edu.artic.db.models.ArticMapFloor
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.pow

/**
 * Description:
 */
class GlideMapTileProvider(private val context: Context,
                           private val floor: ArticMapFloor) : BaseMapTileProvider() {

    override fun getAdjustedTile(x: Int, y: Int, zoom: Int): Tile? {
        return when {
            x >= 0 && y >= 0 -> {
                // calculate tile row count
                val tileXCount = (2.0).pow(zoom)

                // grab which tile based on row, what y number it is in the tile list, x position
                val tileNumber = (y * tileXCount + x).toInt()
                Timber.d("Tile X Count $tileXCount with number $tileNumber")

                val path = "${floor.tiles}zoom$zoom/tiles-$tileNumber.jpg"
                return try {
                    val array = Glide.with(context)
                            .`as`(ByteArray::class.java)
                            .load(path)
                            .submit(TILE_SIZE.toInt(), TILE_SIZE.toInt())
                            .get()
                    Tile(TILE_SIZE.toInt(),
                            TILE_SIZE.toInt(),
                            array
                    )
                } catch (e: IOException) {
                    Timber.e("Could not find tile with $path")
                    null
                }
            }
            else -> {
                null
            }
        }
    }

}