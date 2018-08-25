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
