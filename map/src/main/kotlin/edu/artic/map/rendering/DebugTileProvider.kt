package edu.artic.map.rendering

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.geometry.Point
import com.google.maps.android.projection.SphericalMercatorProjection
import edu.artic.map.TextMarkerGenerator
import edu.artic.map.ZOOM_MIN
import edu.artic.map.museumBounds
import timber.log.Timber
import java.io.ByteArrayOutputStream
import kotlin.math.pow

class DebugTileProvider2(context: Context) : TileProvider {

    private val northEast: Point
    private val southWest: Point
    private val mapProjection = SphericalMercatorProjection(TILE_SIZE)
    private val boundsMap: Map<Int, Bounds>

    init {
        northEast = mapProjection.toPoint(museumBounds.northeast)
        southWest = mapProjection.toPoint(museumBounds.southwest)
        boundsMap = evaluatedBoundsMap
    }

    private val textMarkerGenerator = TextMarkerGenerator(context)

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        val bounds = boundsMap[zoom] ?: throw IllegalStateException("Illegal zoom found $zoom")
        val tileLevel = (zoom - ZOOM_MIN + 2).toInt()
        val adjustedX = x - bounds.minX - 1
        val adjustedY = y - bounds.minY
        Timber.d("Looking for adjusted tile from (x: $adjustedX,y: $adjustedY, z: $tileLevel)")
        return when {
            adjustedX >= 0 && adjustedY >= 0 -> {
                // render coordinates
                synchronized(textMarkerGenerator) {
                    // calculate tile row count
                    val tileXCount = (2.0).pow(tileLevel)

                    // grab which tile based on row, what y number it is in the tile list, x position
                    val tileNumber = (adjustedY * tileXCount + adjustedX).toInt()
                    val bitmap = textMarkerGenerator.makeIcon("($adjustedX, $adjustedY, $tileNumber)")
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val byteArray = stream.toByteArray()
                    bitmap.recycle()
                    return Tile(TILE_SIZE.toInt(), TILE_SIZE.toInt(), byteArray)
                }
            }
            else -> null
        }
    }
}