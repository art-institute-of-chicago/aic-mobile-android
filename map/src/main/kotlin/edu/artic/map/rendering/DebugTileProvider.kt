package edu.artic.map.rendering

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import edu.artic.map.TextMarkerGenerator
import java.io.ByteArrayOutputStream

/**
 * Description: Draws text on the map to show off tiles.
 */
class DebugTileProvider(context: Context) : TileProvider {

    private val textMarkerGenerator = TextMarkerGenerator(context)

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        // render coordinates
        synchronized(textMarkerGenerator) {
            val bitmap = textMarkerGenerator.makeIcon("($x, $y)")
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            bitmap.recycle()
            return Tile(512, 512, byteArray)
        }
    }
}