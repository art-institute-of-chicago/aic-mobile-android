package edu.artic.map.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.Tile
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.pow

/**
 * Description:
 */
class GlideMapTileProvider(private val context: Context,
                           floor: Int) : BaseMapTileProvider(floor) {

    override fun getAdjustedTile(x: Int, y: Int, zoom: Int): Tile? {
        return when {
            x >= 0 && y >= 0 -> {
                // calculate tile row count
                val tileXCount = (2.0).pow(zoom)

                // grab which tile based on row, what y number it is in the tile list, x position
                val tileNumber = (y * tileXCount + x).toInt()
                Timber.d("Tile X Count $tileXCount with number $tileNumber")

                val path = "http://aic-mobile-tours.artic.edu/sites/default/files/floor-maps/tiles/floor$floor/zoom$zoom/tiles-$tileNumber.png"
                return try {
                    val bitmap = Glide.with(context).asBitmap().load(path).submit(TILE_SIZE.toInt(), TILE_SIZE.toInt()).get()
                    val newBitmap = bitmap.copy(Bitmap.Config.RGB_565, true)
                     val stream = ByteArrayOutputStream()
                    newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    val array = stream.toByteArray()
                    stream.reset()
                    newBitmap.recycle()
                    // The above flow is temporary until we get JPEG's provided from endpoint,
                    // which will require an update here.
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