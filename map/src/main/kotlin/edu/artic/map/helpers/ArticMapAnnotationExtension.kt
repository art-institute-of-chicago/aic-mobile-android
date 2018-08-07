package edu.artic.map.helpers

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.map.MapItem

fun ArticMapAnnotation.toLatLng() : LatLng {
    return LatLng(
            latitude?.toDouble() ?: 0.0,
            longitude?.toDouble() ?: 0.0
    )
}

fun ArticMapAnnotation.toMapItem() : MapItem.Annotation {
    return MapItem.Annotation(this, this.floor?.toInt() ?: 1)
}

fun List<ArticMapAnnotation>.mapToMapItem() : List<MapItem.Annotation> {
    return this.map {
        it.toMapItem()
    }
}