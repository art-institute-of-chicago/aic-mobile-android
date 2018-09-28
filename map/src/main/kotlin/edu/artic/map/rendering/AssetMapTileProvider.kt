package edu.artic.map.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.gms.maps.model.Tile
import edu.artic.db.models.ArticMapFloor
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.pow




/**
 * Description:
 */
class AssetMapTileProvider(private val context: Context,
                           private val floor: ArticMapFloor) : BaseMapTileProvider() {

    val defaultTile = Tile(
            TILE_SIZE.toInt(),
            TILE_SIZE.toInt(),
            context.assets.open("tiles/blank-tile.jpg").readBytes())

    override fun getAdjustedTile(x: Int, y: Int, zoom: Int): Tile? {
        return when {
            x >= 0 && y >= 0 -> {
                // calculate tile row count
                val tileXCount = (2.0).pow(zoom)

                // grab which tile based on row, what y number it is in the tile list, x position
                val tileNumber = (y * tileXCount + x).toInt()
                Timber.d("Tile X Count $tileXCount with number $tileNumber")

                val path = "floor${floor.number}/zoom$zoom/tiles-$tileNumber.jpg"
                return try {
                    val stream = context.assets.open(path)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(newBitmap)
                    val alphaPaint = Paint()
                    alphaPaint.alpha = 40
                    canvas.drawBitmap(bitmap, 0f, 0f, alphaPaint)
                    val outStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                    val byteArray = outStream.toByteArray()
                    bitmap.recycle()
                    Tile(TILE_SIZE.toInt(),
                            TILE_SIZE.toInt(),
                            byteArray
                    ).also {
                        Timber.d("Loaded tile with path $path")
                    }
                } catch (e: IOException) {
                    Timber.e("Could not find tile with $path")
                    null
                }
            }
            else -> {
                defaultTile
            }
        }
    }

}