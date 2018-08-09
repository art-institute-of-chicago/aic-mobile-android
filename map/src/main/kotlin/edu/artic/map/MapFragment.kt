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
import edu.artic.map.util.ArticObjectDotMarkerGenerator
import edu.artic.map.util.ArticObjectMarkerGenerator
import edu.artic.map.util.DepartmentMarkerGenerator
import edu.artic.map.util.GalleryNumberMarkerGenerator
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

    lateinit var map: GoogleMap

    private val amenitiesMarkerList = mutableListOf<Marker>()
    private val spaceOrLandmarkMarkerList = mutableListOf<Marker>()
    private val departmentMakers = mutableListOf<Marker>()
    private val galleryMarkers = mutableListOf<Marker>()
    private val fullObjectMarkers = mutableListOf<Marker>()
    private val dotObjectMarkers = mutableListOf<Marker>()

    private lateinit var objectMarkerGenerator: ArticObjectMarkerGenerator
    private lateinit var objectDotMarkerGenerator: ArticObjectDotMarkerGenerator
    private lateinit var galleryNumberGenerator: GalleryNumberMarkerGenerator
    private lateinit var departmentMarkerGenerator: DepartmentMarkerGenerator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        objectMarkerGenerator = ArticObjectMarkerGenerator(view.context)
        objectDotMarkerGenerator = ArticObjectDotMarkerGenerator(view.context)
        galleryNumberGenerator = GalleryNumberMarkerGenerator(view.context)
        departmentMarkerGenerator = DepartmentMarkerGenerator(view.context)

        mapView.onCreate(savedInstanceState)
        MapsInitializer.initialize(view.context)
        mapView.getMapAsync { map ->
            this.map = map
            map.setMinZoomPreference(17f)
            map.setMaxZoomPreference(22f)
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
                    zoom < 18 -> {
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

            map.moveCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                            .target(LatLng(41.879592, -87.622491))
                            .bearing(90f)
                            .tilt(0f)
                            .build()
            ))

            map.setOnMarkerClickListener { marker ->
                var handled = false
                when (marker.tag) {
                    is MapItem.Annotation -> {
                        val annotation = marker.tag as MapItem.Annotation
                        when (annotation.item.annotationType) {
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
                .subscribe { (newPostition, zoomLevel) ->
                    map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    newPostition,
                                    when (zoomLevel) {
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

        viewModel.distinctFloor
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

        viewModel.amenities
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { annotationList ->
                    loadMarkersForAnnotation(
                            annotationList,
                            amenitiesMarkerList
                    ) { mapItem ->
                        val icon  = when(mapItem.item.amenityType) {
                            ArticMapAmenityType.WOMANS_ROOM -> {
                                R.drawable.icon_amenity_map_womens_room_blue
                            }
                            ArticMapAmenityType.MENS_ROOM -> {
                                R.drawable.icon_amenity_map_mens_room_blue
                            }
                            ArticMapAmenityType.ELEVATOR -> {
                                R.drawable.icon_amenity_map_elevator_blue
                            }
                            ArticMapAmenityType.GIFT_SHOP -> {
                                R.drawable.icon_amenity_map_shop_blue
                            }
                            ArticMapAmenityType.TICKETS -> {
                                R.drawable.icon_amenity_map_tickets_blue
                            }
                            ArticMapAmenityType.INFORMATION -> {
                                R.drawable.icon_amenity_map_information_blue
                            }
                            ArticMapAmenityType.CHECK_ROOM -> {
                                R.drawable.icon_amenity_map_check_room_blue
                            }
                            ArticMapAmenityType.AUDIO_GUIDE -> {
                                R.drawable.icon_amenity_map_audio_guide_blue
                            }
                            ArticMapAmenityType.WHEELCHAIR_RAMP -> {
                                R.drawable.icon_amenity_map_wheelchair_ramp_blue
                            }
                            ArticMapAmenityType.DINING -> {
                                R.drawable.icon_amenity_map_restaurant_blue
                            }
                            ArticMapAmenityType.FAMILY_RESTROOM -> {
                                R.drawable.icon_amenity_map_family_restroom_blue
                            }
                            else ->{
                                Timber.d("unknownAmenityType: ${mapItem.item.amenityType}")
                                0
                            }

                        }

                        var options = MarkerOptions()
                                .position(mapItem.item.toLatLng())
                                .zIndex(0f)
                        if(icon != 0) {
                            options = options.icon(BitmapDescriptorFactory.fromResource(icon))
                        }
                        options
                    }
                }
                .disposedBy(disposeBag)

        viewModel.spacesAndLandmarks
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { annotationList ->
                    loadMarkersForAnnotation(
                            annotationList,
                            spaceOrLandmarkMarkerList
                    ) { mapItem ->
                        MarkerOptions()
                                .position(mapItem.item.toLatLng())
                                .icon(BitmapDescriptorFactory.fromBitmap(
                                        galleryNumberGenerator.makeIcon(mapItem.item.label.orEmpty()))
                                ).zIndex(1f)
                    }
                }.disposedBy(disposeBag)


        viewModel.veryDynamicMapItems
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { itemList ->
                    departmentMakers.forEach { marker ->
                        marker.remove()
                    }
                    departmentMakers.clear()
                    Timber.d("DepartmentMarker list cleared")
                    galleryMarkers.forEach { marker ->
                        marker.remove()
                    }
                    galleryMarkers.clear()
                    dotObjectMarkers.forEach { marker ->
                        marker.remove()
                    }
                    dotObjectMarkers.clear()
                    fullObjectMarkers.forEach { marker ->
                        marker.remove()
                    }
                    fullObjectMarkers.clear()

                    itemList.forEach { mapItem ->
                        when (mapItem) {
                            is MapItem.Annotation -> {
                                val annotation = mapItem.item
                                when (annotation.annotationType) {
                                    ArticMapAnnotationType.DEPARTMENT -> {
                                        loadDepartment(mapItem)
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
                    Timber.d("DepartmentMarker list size after itemList for each ${departmentMakers.size}")
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
            galleryMarkers.add(
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
                            .zIndex(1f)
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
                                            .zIndex(2f)
                            )
                            marker.tag = annotation
                            departmentMakers.add(marker)
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
                            val fullMaker = map.addMarker(
                                    MarkerOptions()
                                            .position(articObject.toLatLng())
                                            .icon(BitmapDescriptorFactory.fromBitmap(
                                                    objectMarkerGenerator.makeIcon(resource)
                                            ))
                                            .zIndex(2f)
                                            .visible(true)
                            )
                            fullObjectMarkers.add(fullMaker)
                            val dotMaker = map.addMarker(MarkerOptions()
                                    .position(articObject.toLatLng())
                                    .icon(BitmapDescriptorFactory.fromBitmap(
                                            objectDotMarkerGenerator.makeIcon()
                                    ))
                                    .zIndex(2f)
                                    .visible(false))
                            dotObjectMarkers.add(dotMaker)
                        }
                    }
                })


    }

    private inline fun loadMarkersForAnnotation(annotationList: List<MapItem.Annotation>, list: MutableList<Marker>, markerBuilder: (MapItem.Annotation) -> MarkerOptions) {
        list.forEach {
            it.remove()
        }
        list.clear()
        annotationList.forEach {
            list.add(
                    map.addMarker(
                            markerBuilder(it)
                    )
            )
        }
    }

}