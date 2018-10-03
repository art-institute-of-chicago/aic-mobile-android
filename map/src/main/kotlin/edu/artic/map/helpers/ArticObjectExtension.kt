package edu.artic.map.helpers

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.*

/**
 * See [convertToLatLng].
 *
 * When interacting with [MapItemRenderers][edu.artic.map.rendering.MapItemRenderer],
 * consider using `getAdjustedLocationFromItem` instead.
 */
fun ArticObject.toLatLng(): LatLng {
    return convertToLatLng(location)
}

/**
 * See [convertToLatLng].
 */
fun ArticSearchArtworkObject.toLatLng(): LatLng {
    return convertToLatLng(location)
}

/**
 * See [convertToLatLng].
 */
fun ArticTour.toLatLng(): LatLng {
    return convertToLatLng(location)
}

fun ArticExhibition.toLatLng(): LatLng {
    return LatLng(
            latitude ?: 0.0,
            longitude ?: 0.0
    )
}

fun ArticGallery.toLatLng() : LatLng{
    return LatLng(latitude, longitude)
}

fun ArticMapAnnotation.toLatLng(): LatLng {
    return LatLng(
            latitude?.toDouble() ?: 0.0,
            longitude?.toDouble() ?: 0.0
    )
}