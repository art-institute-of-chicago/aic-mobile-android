package edu.artic.map.helpers

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticMapAnnotation

fun ArticMapAnnotation.toLatLng(): LatLng {
    return LatLng(
            latitude?.toDouble() ?: 0.0,
            longitude?.toDouble() ?: 0.0
    )
}