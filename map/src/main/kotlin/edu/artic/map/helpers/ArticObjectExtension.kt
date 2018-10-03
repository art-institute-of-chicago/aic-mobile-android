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
    return LatLng((latitude!! - 41.879592) + 40.714044, (longitude!! - -87.622491) + -73.9586605)
}

fun ArticGallery.toLatLng() : LatLng{
    return LatLng((latitude - 41.879592) + 40.714044, (longitude - -87.622491) + -73.9586605)
}

fun ArticMapAnnotation.toLatLng(): LatLng {
    val lat = latitude?.toDouble() ?: 0.0
    val lon = longitude?.toDouble() ?: 0.0
    return LatLng(
            (lat - 41.879592) + 40.714044,
            (lon - -87.622491) + -73.9586605
    )
}