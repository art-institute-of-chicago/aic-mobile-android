package edu.artic.map

import android.location.Location
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds


/**
 * Center of the screen, as used by [initialMapCameraPosition].
 *
 * This is _NOT_ the same as the center of [museumBounds] (although it is pretty close).
 */
internal val defaultMapPosition = LatLng(41.879592, -87.622491)

/**
 * Camera update enforcing display of first visible content when a [MapFragment] loads.
 *
 * @see com.google.android.gms.maps.GoogleMap
 */
internal fun initialMapCameraPosition(): CameraUpdate {
    return CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                    .target(defaultMapPosition)
                    .bearing(90f)
                    .tilt(45f)
                    .build())
}

/**
 * Outside boundary of the area we display on the map. This is strictly larger than
 * the museum grounds, as we allow panning and zooming a decent amount away from the
 * buildings themselves.
 */
internal val museumBounds: LatLngBounds = LatLngBounds(
        // southwest
        LatLng(41.875815, -87.627528),

        // northeast
        LatLng(41.883309, -87.617464)
)

fun isLocationInMuseum(location : Location) : Boolean {
    return museumBounds.contains(LatLng(location.latitude, location.longitude))
}