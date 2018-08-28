package edu.artic.map

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * The initial position where the [GoogleMap] starts on load of the [MapFragment2]
 */
internal fun initialMapCameraPosition(): CameraUpdate = CameraUpdateFactory.newCameraPosition(
        CameraPosition.Builder()
                .target(LatLng(41.879592, -87.622491))
                .bearing(90f)
                .tilt(45f)
                .build())

internal val museumBounds: LatLngBounds = LatLngBounds(
        LatLng(41.878423, -87.624189),
        LatLng(41.881612, -87.621000)
)
