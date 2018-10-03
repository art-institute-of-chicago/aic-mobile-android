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
//40.714044,-73.9586605 NYC
//41.879592, -87.622491 AIC
fun convertToLatLng(location: String?): LatLng {
    val split = location?.split(",")
    val lat = split?.first()?.toDouble() ?: 0.0
    val lon = split?.last()?.toDouble() ?: 0.0
    return LatLng(
            (lat - 41.879592) + 40.714044,
            (lon - -87.622491) + -73.9586605
    )
}


