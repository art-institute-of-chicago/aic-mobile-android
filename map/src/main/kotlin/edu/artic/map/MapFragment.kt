package edu.artic.map

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fuzz.rx.disposedBy
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.models.*
import edu.artic.map.helpers.toLatLng
import edu.artic.map.util.ArticObjectMarkerGenerator
import edu.artic.map.util.DepartmentMarkerGenerator
import edu.artic.map.util.GalleryNumberMarkerGenerator
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_map.*
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

    lateinit var map: GoogleMap

    val currentMarkers = mutableListOf<Marker>()

    lateinit var objectMarkerGenerator: ArticObjectMarkerGenerator
    lateinit var galleryNumberGenerator: GalleryNumberMarkerGenerator
    lateinit var departmentMarkerGenerator: DepartmentMarkerGenerator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        objectMarkerGenerator = ArticObjectMarkerGenerator(view.context)
        galleryNumberGenerator = GalleryNumberMarkerGenerator(view.context)
        departmentMarkerGenerator = DepartmentMarkerGenerator(view.context)
        mapView.onCreate(savedInstanceState)
        MapsInitializer.initialize(view.context)
        mapView.getMapAsync { map ->
            this.map = map
            map.setMinZoomPreference(18f)
            map.setMaxZoomPreference(21f)
            /**
             * We are setting the bounds here as they are roughly the bounds of the museum,
             * locks us into just that area
             */
            map.setLatLngBoundsForCameraTarget(
                    LatLngBounds(

                            LatLng(41.878523, -87.623689),
                            LatLng(41.880712, -87.621100)
                    )
            )
            map.setOnCameraIdleListener {
                val zoom = map.cameraPosition.zoom
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
                            is MapItem.Annotation -> {
                                val annotation = mapItem.item
                                when (annotation.annotationType) {
                                    ArticMapAnnotationType.DEPARTMENT -> {
                                        loadDepartment(annotation, mapItem.floor)
                                    }
                                    ArticMapAnnotationType.TEXT  -> {
                                        when (annotation.textType) {
                                            ArticMapTextType.LANDMARK-> {
                                                loadLandmark(annotation)
                                            }
                                            ArticMapTextType.SPACE-> {
                                                loadLandmark(annotation)
                                            }
                                            else -> {
                                                loadGenericAnnotation(annotation)
                                            }
                                        }
                                    }
                                    else -> {
                                        loadGenericAnnotation(annotation)
                                    }
                                }
                            }
                            is MapItem.Gallery -> {
                                val gallery = mapItem.item
                                loadGalleryNumber(gallery)

                            }
                            is MapItem.Object -> {
                                val articObject = mapItem.item
                                loadObject(articObject, mapItem.floor)
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

    override fun onDestroyView() {
        mapView.onDestroy()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    fun loadGalleryNumber(gallery: ArticGallery) {
        gallery.number?.let {
            currentMarkers.add(
                    map.addMarker(MarkerOptions()
                            .position(gallery.toLatLng())
                            .icon(BitmapDescriptorFactory
                                    .fromBitmap(
                                            galleryNumberGenerator
                                                    .makeIcon(
                                                            it
                                                    )
                                    )
                            )
                            .anchor(.5f, 0f)
                    )
            )
        }
    }

    fun loadDepartment(department: ArticMapAnnotation, floor: Int) {
        Glide.with(this)
                .asBitmap()
                .load(department.imageUrl)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        if (viewModel.currentZoomLevel === MapZoomLevel.Two && viewModel.currentFloor == floor) {
                            currentMarkers.add(
                                    map.addMarker(
                                            MarkerOptions()
                                                    .position(department.toLatLng())
                                                    .icon(BitmapDescriptorFactory.fromBitmap(
                                                            departmentMarkerGenerator.makeIcon(
                                                                    resource,
                                                                    department.label.orEmpty())
                                                    ))
                                    )
                            )
                        }
                    }
                })
    }

    fun loadObject(articObject: ArticObject, floor: Int) {
        Glide.with(this)
                .asBitmap()
                .load(articObject.thumbnailFullPath)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        if (viewModel.currentZoomLevel === MapZoomLevel.Three && viewModel.currentFloor == floor) {
                            currentMarkers.add(
                                    map.addMarker(
                                            MarkerOptions()
                                                    .position(articObject.toLatLng())
                                                    .icon(BitmapDescriptorFactory.fromBitmap(
                                                            objectMarkerGenerator.makeIcon(resource)
                                                    ))
                                    )
                            )
                        }
                    }
                })


    }

    fun loadLandmark(annotation: ArticMapAnnotation) {
        currentMarkers.add(
                map.addMarker(MarkerOptions()
                        .position(annotation.toLatLng())
                        .icon(BitmapDescriptorFactory.fromBitmap(
                                galleryNumberGenerator.makeIcon(annotation.label.orEmpty()))
                        )
                )
        )
    }

    fun loadGenericAnnotation(annotation: ArticMapAnnotation) {
        currentMarkers.add(
                map.addMarker(MarkerOptions()
                        .position(annotation.toLatLng())
                )
        )
    }

}