package edu.artic.location

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * Outside boundary of the area we display on the map. This is strictly larger than
 * the museum grounds, as we allow panning and zooming a decent amount away from the
 * buildings themselves.
 */
//40.714044,-73.9586605 NYC
//41.879592, -87.622491 AIC
//val museumBounds: LatLngBounds = LatLngBounds(
//        // southwest
//        LatLng(41.875815, -87.627528),
//
//        // northeast
//        LatLng(41.883309, -87.617464)
//)

val museumBounds: LatLngBounds = LatLngBounds(
        // southwest
        LatLng((41.875815-41.879592) + 40.714044, (87.622491 - 87.627528) + -73.9586605),

        // northeast
        LatLng((41.883309-41.879592) + 40.714044, (87.622491 - 87.617464) + -73.9586605)
)

fun isLocationInMuseum(location : Location) : Boolean {
    return museumBounds.contains(LatLng(location.latitude, location.longitude))
}