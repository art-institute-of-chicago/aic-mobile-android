package edu.artic.map

import android.location.Location
import android.support.annotation.UiThread
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import edu.artic.db.models.ArticObject


/**
 * We only want to display [ArticObject] annotations that are within 15 meters
 * of the center of the map.
 *
 * @param bounds the restrictions of
 * [the map's viewport][com.google.android.gms.maps.Projection.getVisibleRegion]
 */
fun LatLng.isCloseEnoughToCenter(bounds: LatLngBounds): Boolean {
    return bounds.contains(this) && bounds.center.distanceTo(this) < 15
}

/**
 * Alias to [Location.distanceBetween], where 'this' is the first param and 'other' is the second.
 *
 * @return a distance, in meters
 */
fun LatLng.distanceTo(other: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
            this.latitude,
            this.longitude,
            other.latitude,
            other.longitude,
            results
    )
    return results[0]
}

/**
 * Google's Map API throws exceptions if a marker is used when either
 * 1. It is no longer attached to the map
 * 2. It has an invalid icon
 *
 * Situation 1 is vastly more likely; this is the result of issues
 * surrounding synchronization of access to the marker. This method
 * will print a warning when it detects those scenarios instead of
 * crashing the app.
 *
 * FIXME: Fix all such synchronization issues, then remove this method
 *
 * @return true if the operation failed, false if it succeeded
 */
@UiThread
fun <T> Marker.tryExpectingFailure(retry: Boolean = false, action: (Marker) -> T): Boolean {
    return try {
        action(this)
        false
    } catch (ex: IllegalArgumentException) {
        if (retry) {
            return tryExpectingFailure(false, action)
        } else {
            if (BuildConfig.DEBUG) {
                Log.w("MapMarker", ex.message)
            }
            true
        }
    }
}
