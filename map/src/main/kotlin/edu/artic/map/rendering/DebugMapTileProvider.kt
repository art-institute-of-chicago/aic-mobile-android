package edu.artic.map.rendering

import android.graphics.*
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.pow

/**
 * Description:
 */
class DebugMapTileProvider : BaseMapTileProvider() {

    private val paint = Paint().apply {
        this.strokeWidth = 5.0f
        this.color = Color.BLACK
    }

    override fun getAdjustedTile(x: Int, y: Int, zoom: Int): Tile? {
        return when {
            x >= 0 && y >= 0 -> {
                // calculate tile row count
                val tileXCount = (2.0).pow(zoom)

                // grab which tile based on row, what y number it is in the tile list, x position
                val tileNumber = (y * tileXCount + x).toInt()
                Timber.d("Tile X Count $tileXCount with number $tileNumber")

                return try {

                    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_4444)
                    val canvas = Canvas(bitmap)
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(Rect(0, 0, 512, 512), paint)
                    paint.textSize = 40f
                    paint.style = Paint.Style.FILL
                    canvas.drawText("$tileNumber - $zoom", 250f, 250f, paint)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val array = stream.toByteArray()
                    Tile(TILE_SIZE.toInt(),
                            TILE_SIZE.toInt(),
                            array
                    )
                } catch (e: IOException) {
                    TileProvider.NO_TILE
                }
            }
            else -> {
                TileProvider.NO_TILE
            }
        }
    }

}