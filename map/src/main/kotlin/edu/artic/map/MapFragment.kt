package edu.artic.map

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterValue
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import com.jakewharton.rxbinding2.view.clicks
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

    private lateinit var objectMarkerGenerator: ArticObjectMarkerGenerator
    private lateinit var galleryNumberGenerator: GalleryNumberMarkerGenerator
    private lateinit var departmentMarkerGenerator: DepartmentMarkerGenerator

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
            map.isIndoorEnabled = false
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

            map.setOnMarkerClickListener { marker ->
                var handled = false
                when(marker.tag) {
                    is MapItem.Annotation -> {
                        val annotation = marker.tag as MapItem.Annotation
                        when(annotation.item.annotationType) {
                            ArticMapAnnotationType.DEPARTMENT -> {
                                viewModel.departmentMarkerSelected(annotation.item)
                                handled = true
                            }
                        }
                    }
                }
                return@setOnMarkerClickListener handled
            }
        }
    }

    override fun setupBindings(viewModel: MapViewModel) {
        lowerLevel.clicks()
                .subscribe { viewModel.floorChangedTo(0) }
                .disposedBy(disposeBag)
        floorOne.clicks()
                .subscribe { viewModel.floorChangedTo(1) }
                .disposedBy(disposeBag)
        floorTwo.clicks()
                .subscribe { viewModel.floorChangedTo(2) }
                .disposedBy(disposeBag)
        floorThree.clicks()
                .subscribe { viewModel.floorChangedTo(3) }
                .disposedBy(disposeBag)

        viewModel.cameraMovementRequested
                .filterValue()
                .subscribe {(newPostition, zoomLevel) ->
                    map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    newPostition,
                                    when(zoomLevel) {
                                        MapZoomLevel.One -> {
                                            18.0f
                                        }
                                        MapZoomLevel.Two -> {
                                            19.0f
                                        }
                                        MapZoomLevel.Three -> {
                                            20.0f
                                        }
                                    }
                            )
                    )
                }.disposedBy(disposeBag)

        viewModel.floor
                .subscribe {
                    lowerLevel.setBackgroundResource(
                            if (it == 0)
                                R.drawable.map_floor_background_selected
                            else
                                R.drawable.map_floor_background_default
                    )
                    floorOne.setBackgroundResource(
                            if (it == 1)
                                R.drawable.map_floor_background_selected
                            else
                                R.drawable.map_floor_background_default
                    )
                    floorTwo.setBackgroundResource(
                            if (it == 2)
                                R.drawable.map_floor_background_selected
                            else
                                R.drawable.map_floor_background_default
                    )
                    floorThree.setBackgroundResource(
                            if (it == 3)
                                R.drawable.map_floor_background_selected
                            else
                                R.drawable.map_floor_background_default
                    )
                }
                .disposedBy(disposeBag)

        viewModel.mapAnnotations
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    currentMarkers.forEach { marker ->
                        marker.remove()
                    }
                    currentMarkers.clear()

                    it.forEach { mapItem ->
                        when (mapItem) {
                            is MapItem.Annotation -> {
                                val annotation = mapItem.item
                                when (annotation.annotationType) {
                                    ArticMapAnnotationType.DEPARTMENT -> {
                                        loadDepartment(mapItem)
                                    }
                                    ArticMapAnnotationType.TEXT -> {
                                        when (annotation.textType) {
                                            ArticMapTextType.LANDMARK -> {
                                                loadLandmark(annotation)
                                            }
                                            ArticMapTextType.SPACE -> {
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

    private fun loadGalleryNumber(gallery: ArticGallery) {
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

    private fun loadDepartment(annotation: MapItem.Annotation) {
        val department = annotation.item
        val floor = annotation.floor
        Glide.with(this)
                .asBitmap()
                .load(department.imageUrl)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        if (viewModel.currentZoomLevel === MapZoomLevel.Two && viewModel.currentFloor == floor) {
                            val marker = map.addMarker(
                                    MarkerOptions()
                                            .position(department.toLatLng())
                                            .icon(BitmapDescriptorFactory.fromBitmap(
                                                    departmentMarkerGenerator.makeIcon(
                                                            resource,
                                                            department.label.orEmpty())
                                            ))
                            )
                            marker.tag = annotation
                            currentMarkers.add(marker)
                        }
                    }
                })
    }

    private fun loadObject(articObject: ArticObject, floor: Int) {
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

    private fun loadLandmark(annotation: ArticMapAnnotation) {
        currentMarkers.add(
                map.addMarker(MarkerOptions()
                        .position(annotation.toLatLng())
                        .icon(BitmapDescriptorFactory.fromBitmap(
                                galleryNumberGenerator.makeIcon(annotation.label.orEmpty()))
                        )
                )
        )
    }

    private fun loadGenericAnnotation(annotation: ArticMapAnnotation) {
        currentMarkers.add(
                map.addMarker(MarkerOptions()
                        .position(annotation.toLatLng())
                )
        )
    }

}