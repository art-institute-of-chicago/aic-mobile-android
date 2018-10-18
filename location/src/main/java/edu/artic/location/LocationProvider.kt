package edu.artic.location

import android.location.Location
import io.reactivex.subjects.Subject

interface LocationProvider {

    val currentLocation: Subject<Location>

    val isTrackingLocationChanges: Subject<Boolean>

    fun startLocationTracking()

    fun stopLocationTracking()
}