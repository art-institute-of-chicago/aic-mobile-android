package edu.artic.map.rendering

import com.google.android.gms.maps.model.LatLng

/**
 * Description: Calculates the map coordinates as relative to earth. This is useful for conversion
 * by mapping the bounds of the tiles to the bounds of the earth. We translate a [LatLng] on the whole
 * earth to one that exists on the smaller map.
 */
class MapProjectionHelper {

    private val end: LatLng = LatLng(41.880196, -87.623028)
    private val start: LatLng = LatLng(41.880020, -87.623989)
    private val differenceLat = end.latitude - start.latitude
    private val differenceLon = end.longitude - start.longitude

    fun toSmallMapLatLng(latLng: LatLng): LatLng {
        return LatLng(latLng.latitude, latLng.longitude)
    }
}