package edu.artic.location

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * Outside boundary of the area we display on the map. This is strictly larger than
 * the museum grounds, as we allow panning and zooming a decent amount away from the
 * buildings themselves.
 */
val museumBounds: LatLngBounds = LatLngBounds(
        // southwest
        LatLng(41.875815, -87.627528),

        // northeast
        LatLng(41.883309, -87.617464)
)

/**
 * Center of the screen, as used by [initialMapCameraPosition].
 *
 * This is _NOT_ the same as the center of [museumBounds] (although it is pretty close).
 */
val centerOfMuseumOnMap = LatLng(41.879592, -87.622491)

const val radiusFromCenterOfMuseumInMeters = 250.0f

fun isLocationInMuseum(location : Location) : Boolean {
    val distance = FloatArray(1)
    Location.distanceBetween(location.latitude, location.longitude, centerOfMuseumOnMap.latitude, centerOfMuseumOnMap.longitude, distance)
    return distance[0] <= radiusFromCenterOfMuseumInMeters
}