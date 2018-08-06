package edu.artic.map

import android.os.Bundle
import android.view.View
import com.fuzz.rx.disposedBy
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_map.*
import timber.log.Timber
import kotlin.reflect.KClass

class MapFragment : BaseViewModelFragment<MapViewModel>() {

    override val viewModelClass: KClass<MapViewModel>
        get() = MapViewModel::class
    override val title: String
        get() = ""
    override val layoutResId: Int
        get() = R.layout.fragment_map
    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.Map
    lateinit var destroyableMapView: MapView
    lateinit var map: GoogleMap

    val currentMarkers = mutableListOf<Marker>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        destroyableMapView = mapView
        mapView.onCreate(savedInstanceState)
        MapsInitializer.initialize(view.context)
        mapView.getMapAsync { map ->
            this.map = map
            map.setMinZoomPreference(18f)
            map.setMaxZoomPreference(21f)
            map.setLatLngBoundsForCameraTarget(
                    LatLngBounds(

                            LatLng(41.878523, -87.623689),
                            LatLng(41.880712, -87.621100)
                    )
            )
            map.setOnCameraIdleListener {
                val zoom = map.cameraPosition.zoom
                Timber.d("MapZoomLevel: $zoom")
                when {
                    zoom < 18.5 -> {
                        viewModel.zoomLevelChangedTo(MapZoomLevel.One)
                    }
                    zoom < 20 -> {
                        viewModel.zoomLevelChangedTo(MapZoomLevel.Two)
                    }
                    else -> {
                        viewModel.zoomLevelChangedTo(MapZoomLevel.Three)
                    }
                }
            }
        }
    }

    override fun setupBindings(viewModel: MapViewModel) {
        viewModel.mapAnnotations
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    currentMarkers.forEach {
                        it.remove()
                    }
                    currentMarkers.clear()

                    it.forEach { mapItem ->
                        when (mapItem) {
                            is MapItem.MapAnnotation -> {
                                val annotation = mapItem.annotation
                                currentMarkers.add(
                                        map.addMarker(
                                                MarkerOptions()
                                                        .title(annotation.label)
                                                        .position(
                                                                LatLng(
                                                                        annotation.latitude!!.toDouble(),
                                                                        annotation.longitude!!.toDouble()
                                                                )
                                                        )
                                        )
                                )
                            }
                        }


                    }
                }.disposedBy(disposeBag)
    }


    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        destroyableMapView.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}