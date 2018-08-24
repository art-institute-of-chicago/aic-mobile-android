package edu.artic.map.helpers

import android.support.annotation.UiThread
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.map.BuildConfig

fun ArticObject.toLatLng(): LatLng {
    val split = location?.split(",")
    return LatLng(
            split?.first()?.toDouble() ?: 0.0,
            split?.last()?.toDouble() ?: 0.0
    )
}

fun ArticTour.toLatLng(): LatLng {
    val split = location?.split(",")
    return LatLng(
            split?.first()?.toDouble() ?: 0.0,
            split?.last()?.toDouble() ?: 0.0
    )
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
fun <T> Marker.ifNotRemoved(retry: Boolean = false, action: (Marker) -> T) : Boolean {
    return try {
        action(this)
        false
    } catch (ex: IllegalArgumentException) {
        if (retry) {
            return this.ifNotRemoved(false, action)
        } else {
            if (BuildConfig.DEBUG) {
                Log.w("MapMarker", ex.message)
            }
            true
        }
    }
}