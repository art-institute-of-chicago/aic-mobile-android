package edu.artic.map

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * The initial position where the [GoogleMap] starts on load of the [MapFragment]
 */
internal fun initialMapCameraPosition(): CameraUpdate = CameraUpdateFactory.newCameraPosition(
        CameraPosition.Builder()
                .target(LatLng(41.879592, -87.622491))
                .bearing(90f)
                .tilt(45f)
                .build())

internal val museumBounds: LatLngBounds = LatLngBounds(
        // southwest
        LatLng(41.875815, -87.627528),

        // northeast
        LatLng(41.883309, -87.617464)
)