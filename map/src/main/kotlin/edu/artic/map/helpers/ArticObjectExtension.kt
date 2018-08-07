package edu.artic.map.helpers

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticObject

fun ArticObject.toLatLng(): LatLng {
    return LatLng(
            latitude,
            longitude
    )
}