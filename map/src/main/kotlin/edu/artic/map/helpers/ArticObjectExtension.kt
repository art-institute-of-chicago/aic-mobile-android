package edu.artic.map.helpers

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticObject

fun ArticObject.toLatLng(): LatLng {
    val split = location?.split(",")
    return LatLng(
            split?.first()?.toDouble() ?: 0.0,
            split?.last()?.toDouble() ?: 0.0
    )
}