package edu.artic.map

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng


/**
 * Center of the screen, as used by [initialMapCameraPosition].
 *
 * This is _NOT_ the same as the center of [museumBounds] (although it is pretty close).
 */
//40.714044,-73.9586605 NYC
//41.879592, -87.622491 AIC
//internal val defaultMapPosition = LatLng(41.879592, -87.622491)
internal val defaultMapPosition = LatLng(40.714044,-73.9586605)

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