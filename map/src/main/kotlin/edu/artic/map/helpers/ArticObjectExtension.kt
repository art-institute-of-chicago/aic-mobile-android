package edu.artic.map.helpers

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticObject

fun ArticObject.toLatLng(): LatLng {
    val location = Location(location)
    return LatLng(
            location.latitude,
            location.longitude
    )
}