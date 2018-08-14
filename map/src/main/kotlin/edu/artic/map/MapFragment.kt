package edu.artic.map

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.annotation.UiThread
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
import edu.artic.base.utils.isResourceConstrained
import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticMapAmenityType
import edu.artic.db.models.ArticMapAnnotationType
import edu.artic.db.models.ArticObject
import edu.artic.map.helpers.toLatLng
import edu.artic.map.util.ArticObjectDotMarkerGenerator
import edu.artic.map.util.ArticObjectMarkerGenerator
import edu.artic.map.util.DepartmentMarkerGenerator
import edu.artic.map.util.GalleryNumberMarkerGenerator
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_map.*
import timber.log.Timber
import java.io.InputStream
import kotlin.reflect.KClass

/**
 * This Fragment contains a [GoogleMap] with a custom tileset and quite a few markers.
 *
 * We support 3 distinct zoom levels at the moment:
 * * [MapZoomLevel.One] shows as much of the museum as possible, with markers for
 * [Spaces and Landmarks][MapViewModel.spacesAndLandmarks]
 * * [MapZoomLevel.Two] shows markers for [departments][DepartmentMarkerGenerator]
 * * [MapZoomLevel.Three] shows markers for specific [Galleries][MapItem.Gallery] and
 * [miscellaneous ArticObjects][MapItem.Object]s
 *
 * Note that (in keeping with our standard architecture) much of the complexity is
 * delegated through [MapViewModel].
 *
 * @see [MapActivity]
 */
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
    private val departmentMarkers = mutableListOf<Marker>()
    private val galleryMarkers = mutableListOf<Marker>()
    private val fullObjectMarkers = mutableListOf<Marker>()
    private val dotObjectMarkers = mutableListOf<Marker>()

    private lateinit var objectMarkerGenerator: ArticObjectMarkerGenerator
    private lateinit var objectDotMarkerGenerator: ArticObjectDotMarkerGenerator
    private lateinit var galleryNumberGenerator: GalleryNumberMarkerGenerator
    private lateinit var departmentMarkerGenerator: DepartmentMarkerGenerator

    private lateinit var baseGroundOverlay: GroundOverlay
    private lateinit var buildingGroundOverlay: GroundOverlay
    private var groundOverlayGenerated: Subject<Boolean> = BehaviorSubject.createDefault(false)

    companion object {
        const val OBJECT_DETAILS = "object-details"
    }

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
            map.isBuildingsEnabled = false
            map.isIndoorEnabled = false
            map.isTrafficEnabled = false
            map.setMapStyle(
                    //Leaving this here for now; we will pull from raw resource folder or assets
                    // folder at a later time. Or possibly just turn it into a helper constant
                    MapStyleOptions("[\n" +
                            "  {\n" +
                            "    \"elementType\": \"labels\",\n" +
                            "    \"stylers\": [\n" +
                            "      {\n" +
                            "        \"visibility\": \"off\"\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"featureType\": \"administrative\",\n" +
                            "    \"elementType\": \"geometry\",\n" +
                            "    \"stylers\": [\n" +
                            "      {\n" +
                            "        \"visibility\": \"off\"\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"featureType\": \"administrative.land_parcel\",\n" +
                            "    \"stylers\": [\n" +
                            "      {\n" +
                            "        \"visibility\": \"off\"\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"featureType\": \"administrative.neighborhood\",\n" +
                            "    \"stylers\": [\n" +
                            "      {\n" +
                            "        \"visibility\": \"off\"\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"featureType\": \"poi\",\n" +
                            "    \"stylers\": [\n" +
                            "      {\n" +
                            "        \"visibility\": \"off\"\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"featureType\": \"road\",\n" +
                            "    \"stylers\": [\n" +
                            "      {\n" +
                            "        \"visibility\": \"off\"\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"featureType\": \"road\",\n" +
                            "    \"elementType\": \"labels.icon\",\n" +
                            "    \"stylers\": [\n" +
                            "      {\n" +
                            "        \"visibility\": \"off\"\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"featureType\": \"transit\",\n" +
                            "    \"stylers\": [\n" +
                            "      {\n" +
                            "        \"visibility\": \"off\"\n" +
                            "      }\n" +
                            "    ]\n" +
                            "  }\n" +
                            "]")
            )
            map.setMinZoomPreference(17f)
            map.setMaxZoomPreference(22f)
            /**
             * We are setting the bounds here as they are roughly the bounds of the museum,
             * locks us into just that area
             */
            map.setLatLngBoundsForCameraTarget(
                    LatLngBounds(
                            LatLng(41.878423, -87.624189),
                            LatLng(41.881612, -87.621000)
                    )
            )

            baseGroundOverlay = map.addGroundOverlay(
                    GroundOverlayOptions()
                            .positionFromBounds(
                                    LatLngBounds(
                                            LatLng(41.874620, -87.629243),
                                            LatLng(41.884753, -87.615841)
                                    )
                            ).image(deriveBaseOverlayDescriptor(requireActivity()))
                            .zIndex(0.1f)
                    //TODO: Load image from network instead of from Assets
            )
            buildingGroundOverlay = map.addGroundOverlay(
                    GroundOverlayOptions()
                            .positionFromBounds(
                                    LatLngBounds(
                                            LatLng(41.878467, -87.624127),
                                            LatLng(41.880730, -87.621027)
                                    )
                            ).zIndex(.11f)
                            .image(BitmapDescriptorFactory.fromAsset("AIC_Floor1.png"))
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

            map.setOnMapClickListener {
                val objectDetailsFragment = requireActivity().supportFragmentManager?.findFragmentByTag(OBJECT_DETAILS)
                objectDetailsFragment?.let { fragment ->
                    requireActivity().supportFragmentManager
                            ?.beginTransaction()
                            ?.remove(fragment)
                            ?.commit()
                }
            }

            map.setOnMarkerClickListener { marker ->
                when (marker.tag) {
                    is MapItem.Annotation -> {
                        val annotation = marker.tag as MapItem.Annotation
                        when (annotation.item.annotationType) {
                            ArticMapAnnotationType.DEPARTMENT -> {
                                viewModel.departmentMarkerSelected(annotation.item)
                            }
                        }
                    }
                    is ArticObject -> {
                        val mapObject = marker.tag as ArticObject
                        viewModel.articObjectSelected(mapObject)

                    }
                    else -> {
                        map.animateCamera(
                                CameraUpdateFactory.newLatLng(marker.position)
                        )
                    }
                }
                return@setOnMarkerClickListener true
            }
            groundOverlayGenerated.onNext(true)
        }
    }

    @UiThread
    protected fun deriveBaseOverlayDescriptor(host: Activity): BitmapDescriptor {
        val baseOverlayFilename = "AIC_MapBG.jpg"

        return if (host.isResourceConstrained()) {
            val stream: InputStream = host.assets.open(baseOverlayFilename)
            stream.use { imageStream ->
                BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeStream(
                        imageStream,
                        null,
                        BitmapFactory.Options().apply {
                            inSampleSize = 4
                        }
                ))
            }
        } else {
            BitmapDescriptorFactory.fromAsset(baseOverlayFilename)
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
                .subscribe { (newPosition, zoomLevel) ->
                    map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    newPosition,
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

        Observables.combineLatest(
                viewModel.distinctFloor,
                groundOverlayGenerated.filter { it }
        ) { floor, _ ->
            floor
        }.subscribe {
            buildingGroundOverlay.setImage(BitmapDescriptorFactory.fromAsset("AIC_Floor$it.png"))
        }.disposedBy(disposeBag)

        viewModel.amenities
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { annotationList ->
                    loadMarkersForAnnotation(
                            annotationList,
                            amenitiesMarkerList
                    ) { mapItem ->
                        val icon = when (mapItem.item.amenityType) {
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
                            ArticMapAmenityType.MEMBERS_LOUNGE -> {
                                R.drawable.icon_amenity_map_cafe_blue
                            }
                            else -> {
                                Timber.d("unknownAmenityType: ${mapItem.item.amenityType}")
                                0
                            }

                        }

                        var options = MarkerOptions()
                                .position(mapItem.item.toLatLng())
                                .zIndex(0f)
                        if (icon != 0) {
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
                    departmentMarkers.forEach { marker ->
                        marker.remove()
                    }
                    departmentMarkers.clear()
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
                    Timber.d("DepartmentMarker list size after 'itemList.forEach{}': ${departmentMarkers.size}")
                }.disposedBy(disposeBag)

        viewModel.selectedArticObject
                .subscribe { selectedArticObject ->
                    val isMapObjectVisible = objectDetailsContainer.childCount != 0
                    val fragmentManager = requireActivity().supportFragmentManager
                    if (!isMapObjectVisible) {
                        val fragment = MapObjectDetailsFragment.create(selectedArticObject)
                        fragmentManager.beginTransaction()
                                .add(R.id.objectDetailsContainer, fragment, OBJECT_DETAILS)
                                .commit()
                    } else {
                        fragmentManager.beginTransaction()
                                .replace(R.id.objectDetailsContainer, MapObjectDetailsFragment.create(selectedArticObject), OBJECT_DETAILS)
                                .commit()
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
        groundOverlayGenerated.onNext(false)
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
                            departmentMarkers.add(marker)
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

                            fullMaker.tag = articObject

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