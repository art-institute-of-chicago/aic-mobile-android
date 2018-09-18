package edu.artic.map.helpers

import com.google.android.gms.maps.model.LatLng

/**
 * Extension method to convert from a comma-separated string to a [LatLng]. Useful
 * for [edu.artic.db.models.ArticObject] and similar models.
 *
 * Expected format:
 *
 *     50.123,-60.123
 */
fun convertToLatLng(location: String?): LatLng {
    val split = location?.split(",")
    return LatLng(
            split?.first()?.toDouble() ?: 0.0,
            split?.last()?.toDouble() ?: 0.0
    )
}


