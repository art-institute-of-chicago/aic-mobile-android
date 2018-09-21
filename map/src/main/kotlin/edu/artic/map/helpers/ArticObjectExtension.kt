package edu.artic.map.helpers

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.db.models.ArticTour

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
