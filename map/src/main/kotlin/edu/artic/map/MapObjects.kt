package edu.artic.map

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import edu.artic.location.centerOfMuseumOnMap


/**
 * Camera update enforcing display of first visible content when a [MapFragment] loads.
 *
 * @see com.google.android.gms.maps.GoogleMap
 */
internal fun initialMapCameraPosition(): CameraUpdate {
    return CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                    .target(centerOfMuseumOnMap)
                    .bearing(88.725f)
                    .tilt(60f)
                    .zoom(ZOOM_INITIAL)
                    .build())
}