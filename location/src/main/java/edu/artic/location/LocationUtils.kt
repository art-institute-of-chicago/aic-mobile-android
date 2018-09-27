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

fun isLocationInMuseum(location : Location) : Boolean {
    return museumBounds.contains(LatLng(location.latitude, location.longitude))
}