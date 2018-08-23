package edu.artic.map

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.annotation.AnyThread
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fuzz.rx.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.fileAsString
import edu.artic.base.utils.isResourceConstrained
import edu.artic.base.utils.loadBitmap
import edu.artic.base.utils.loadWithThumbnail
import edu.artic.base.utils.statusBarHeight
import edu.artic.db.models.*
import edu.artic.map.carousel.TourCarouselFragment
import edu.artic.map.helpers.toLatLng
import edu.artic.map.util.ArticObjectDotMarkerGenerator
import edu.artic.map.util.ArticObjectMarkerGenerator
import edu.artic.map.util.DepartmentMarkerGenerator
import edu.artic.map.util.GalleryNumberMarkerGenerator
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_map.*
import timber.log.Timber
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
    private var mapClicks: Subject<Boolean> = PublishSubject.create()
    private val tourArgument: Subject<ArticTour> = BehaviorSubject.create()

    companion object {
        const val OBJECT_DETAILS = "object-details"
        const val ZOOM_LEVEL_ONE = 18.0f
        const val ZOOM_LEVEL_TWO = 19.0f
        const val ZOOM_LEVEL_THREE = 20.0f
    }

    override fun onRegisterViewModel(viewModel: MapViewModel) {
        tourArgument
                .mapOptional()
                .bindTo(viewModel.tour)
                .disposedBy(disposeBag)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        objectMarkerGenerator = ArticObjectMarkerGenerator(view.context)
        objectDotMarkerGenerator = ArticObjectDotMarkerGenerator(view.context)
        galleryNumberGenerator = GalleryNumberMarkerGenerator(view.context)
        departmentMarkerGenerator = DepartmentMarkerGenerator(view.context)

        mapView.onCreate(savedInstanceState)
        MapsInitializer.initialize(view.context)
        val mapStyleOptions = requireActivity().assets.fileAsString(filename = "google_map_config.json")
        mapView.getMapAsync { map ->
            this.map = map
            map.isBuildingsEnabled = false
            map.isIndoorEnabled = false
            map.isTrafficEnabled = false
            map.setMapStyle(MapStyleOptions(mapStyleOptions))
            map.setMinZoomPreference(17f)
            map.setMaxZoomPreference(22f)
            /** Adding padding to map so that StatusBar doesn't overlap the compass .**/
            map.setPadding(0, requireActivity().statusBarHeight, 0, 0)
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
                            .tilt(45f)
                            .build()
            ))


            /**
             * Funneling map click event into the mapClicks Observer so that it could be combined
             * with other Observable stream.
             */
            map.setOnMapClickListener {
                mapClicks.onNext(true)
            }


            /**
             * Remove the map object details fragment when user taps outside of object.
             */
            mapClicks
                    .defaultThrottle()
                    .withLatestFrom(viewModel.displayMode) { _, mapMode ->
                        mapMode
                    }.subscribe { mapMode ->
                        when (mapMode) {
                            is MapViewModel.DisplayMode.CurrentFloor -> {
                                val supportFragmentManager = this.requireFragmentManager()
                                supportFragmentManager
                                        .findFragmentByTag(OBJECT_DETAILS)
                                        ?.let {
                                            supportFragmentManager
                                                    .beginTransaction()
                                                    .remove(it)
                                                    .commit()
                                        }
                            }

                            is MapViewModel.DisplayMode.Tour -> {
                                /*do nothing*/
                            }
                        }

                    }.disposedBy(disposeBag)


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
                        map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
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

    /**
     * This method returns a simple accessor for the [baseGroundOverlay]'s [Bitmap].
     * We implicitly pass this over to the Google maps part of Google Play Services
     * via a [android.os.Binder] call, which is probably why the API doesn't support
     * vector images or non-Bitmap images.
     *
     * On low-end devices, [GoogleMap] will run out of memory rendering all of
     * its stuff. We can prevent that by loading the aforementioned Bitmap
     * ourselves at a low sample size - the baseOverlay is by far the largest
     * image (in terms of resolution) that we're displaying.
     *
     * @param host whatever is responsible for displaying this [MapFragment]
     * @see [GroundOverlayOptions.image]
     */
    @AnyThread
    protected fun deriveBaseOverlayDescriptor(host: Context): BitmapDescriptor {
        val baseOverlayFilename = "AIC_MapBG.jpg"

        return if (host.isResourceConstrained()) {
            val baseOverlayBitmap: Bitmap = host.assets.loadBitmap(baseOverlayFilename, 4)

            BitmapDescriptorFactory.fromBitmap(baseOverlayBitmap)
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

        viewModel.displayMode
                .filterFlatMap({ it is MapViewModel.DisplayMode.Tour }, { it as MapViewModel.DisplayMode.Tour })
                .subscribe { mapMode ->
                    val tour = mapMode.active
                    val fragmentManager = requireActivity().supportFragmentManager
                    fragmentManager.beginTransaction()
                            .replace(R.id.infocontainer, TourCarouselFragment.create(tour), OBJECT_DETAILS)
                            .commit()
                }.disposedBy(disposeBag)


        viewModel.cameraMovementRequested
                .filterValue()
                .subscribe { (newPosition, zoomLevel) ->
                    map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    newPosition,
                                    when (zoomLevel) {
                                        MapZoomLevel.One -> {
                                            ZOOM_LEVEL_ONE
                                        }
                                        MapZoomLevel.Two -> {
                                            ZOOM_LEVEL_TWO
                                        }
                                        MapZoomLevel.Three -> {
                                            ZOOM_LEVEL_THREE
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


        viewModel.whatToDisplayOnMap
                .withLatestFrom(viewModel.displayMode) { whatToDisplayOnMap, mapMode ->
                    mapMode to whatToDisplayOnMap
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { mapModeWithItemList ->
                    val mapMode = mapModeWithItemList.first
                    val itemList = mapModeWithItemList.second

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
                                loadObject(articObject, mapItem.floor, mapMode)
                            }
                            is MapItem.TourIntro -> {
                                val articTour = mapItem.item
                                loadTourObject(articTour, mapItem.floor, mapMode)
                            }

                        }
                    }
                    Timber.d("DepartmentMarker list size after 'itemList.forEach{}': ${departmentMarkers.size}")
                }.disposedBy(disposeBag)

        viewModel.selectedArticObject
                .withLatestFrom(viewModel.displayMode) { articObject, mapMode ->
                    mapMode to articObject
                }.subscribe { mapModeWithObject ->
                    val selectedArticObject = mapModeWithObject.second
                    val mapMode = mapModeWithObject.first

                    when (mapMode) {
                        is MapViewModel.DisplayMode.CurrentFloor -> {
                            /**
                             * Display the selected object details.
                             */
                            val fragmentManager = requireActivity().supportFragmentManager
                            fragmentManager.beginTransaction()
                                    .replace(R.id.infocontainer, MapObjectDetailsFragment.create(selectedArticObject), OBJECT_DETAILS)
                                    .commit()
                        }
                        is MapViewModel.DisplayMode.Tour -> {
                            /* do nothing */
                        }
                    }
                }.disposedBy(disposeBag)

        /**
         * Center the full object marker in the map.
         * When we are in [DisplayMode.Tour] and if the item is being centered, always reset the zoom level to MapZoomLevel.Three
         */
        viewModel
                .centerFullObjectMarker
                .subscribe { nid ->
                    fullObjectMarkers.firstOrNull { marker ->
                        val tag = marker.tag
                        tag is ArticObject && tag.nid == nid
                    }?.let { marker ->
                        val currentZoomLevel = map.cameraPosition.zoom
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, Math.max(ZOOM_LEVEL_THREE, currentZoomLevel)))
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

    private fun getAlphaValue(displayMode: MapViewModel.DisplayMode, floor: Int): Float {
        return when (displayMode) {
            is MapViewModel.DisplayMode.CurrentFloor -> 1.0f
            is MapViewModel.DisplayMode.Tour -> {
                if (viewModel.currentFloor == floor) {
                    1.0f
                } else {
                    0.6f
                }
            }
        }
    }

    private fun loadObject(articObject: ArticObject, floor: Int, displayMode: MapViewModel.DisplayMode) {
        Glide.with(this)
                .asBitmap()
                // The 'objectMarkerGenerator' used by the below target only supports bitmaps rendered in software
                .apply(RequestOptions().disallowHardwareConfig())
                .loadWithThumbnail(articObject.thumbnailFullPath, articObject.fullImageFullPath)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        /**
                         * If map display mode is Tour, get the order number of the stop.
                         */
                        if (viewModel.currentFloor == floor || displayMode is MapViewModel.DisplayMode.Tour) {
                            var order: String? = null
                            if (displayMode is MapViewModel.DisplayMode.Tour) {
                                /**
                                 * If map's display mode is Tour, get the order number of the stop.
                                 */
                                val index = displayMode.active
                                        .tourStops
                                        .map { it.objectId }
                                        .indexOf(articObject.nid)

                                if (index > -1) {
                                    order = (index + 1).toString()
                                }
                            }

                            val fullMaker = map.addMarker(
                                    MarkerOptions()
                                            .position(articObject.toLatLng())
                                            .icon(BitmapDescriptorFactory.fromBitmap(
                                                    objectMarkerGenerator.makeIcon(resource, order)
                                            ))
                                            .zIndex(2f)
                                            .visible(true)
                                            .alpha(getAlphaValue(displayMode, floor))/* If the tour is not in the current floor make the ui translucent*/
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

    /**
     * Loads the tour intro object as the marker in the map.
     */
    private fun loadTourObject(articTour: ArticTour, floor: Int, displayMode: MapViewModel.DisplayMode) {
        Glide.with(this)
                .asBitmap()
                .load(articTour.thumbnailFullPath)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        if (displayMode is MapViewModel.DisplayMode.Tour) {

                            val markerAlpha = if (viewModel.currentFloor == floor) {
                                1.0f
                            } else {
                                0.6f
                            }

                            val fullMaker = map.addMarker(
                                    MarkerOptions()
                                            .position(articTour.toLatLng())
                                            .icon(BitmapDescriptorFactory.fromBitmap(
                                                    objectMarkerGenerator.makeIcon(imageViewBitmap = resource)
                                            ))
                                            .zIndex(2f)
                                            .visible(true)
                                            .alpha(markerAlpha)
                            )
                            fullObjectMarkers.add(fullMaker)
                            fullMaker.tag = articTour
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

    override fun setupNavigationBindings(viewModel: MapViewModel) {
        super.setupNavigationBindings(viewModel)
        val tour = requireActivity().intent?.extras?.getParcelable<ArticTour>(MapActivity.ARG_TOUR)

        tour?.let {
            tourArgument.onNext(tour)
        }
    }
}