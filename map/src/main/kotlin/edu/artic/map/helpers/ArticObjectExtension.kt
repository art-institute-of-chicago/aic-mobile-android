package edu.artic.map.helpers

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour

/**
 * See [convertToLatLng].
 */
fun ArticObject.toLatLng(): LatLng {
    return convertToLatLng(location)
}

/**
 * See [convertToLatLng].
 */
fun ArticTour.toLatLng(): LatLng {
    return convertToLatLng(location)
}
