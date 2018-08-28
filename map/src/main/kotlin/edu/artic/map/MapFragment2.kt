package edu.artic.map

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.annotation.AnyThread
import android.view.View
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.fuzz.rx.filterValue
import com.fuzz.rx.withLatestFromOther
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.fileAsString
import edu.artic.base.utils.isResourceConstrained
import edu.artic.base.utils.loadBitmap
import edu.artic.base.utils.statusBarHeight
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapAnnotationType
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.map.carousel.TourCarouselFragment
import edu.artic.map.rendering.MarkerMetaData
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_map.*
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
class MapFragment2 : BaseViewModelFragment<MapViewModel2>() {

    override val viewModelClass: KClass<MapViewModel2>
        get() = MapViewModel2::class
    override val title: String
        get() = ""
    override val layoutResId: Int
        get() = R.layout.fragment_map
    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.Map

    lateinit var map: GoogleMap

    private lateinit var baseGroundOverlay: GroundOverlay
    private lateinit var buildingGroundOverlay: GroundOverlay
    private var groundOverlayGenerated: Subject<Boolean> = BehaviorSubject.createDefault(false)
    private var mapClicks: Subject<Boolean> = PublishSubject.create()

    private val tour: ArticTour? by lazy { requireActivity().intent?.extras?.getParcelable<ArticTour>(MapActivity.ARG_TOUR) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)
        viewModel.mapMarkerConstructor.associateContext(view.context)

        MapsInitializer.initialize(view.context)
        val mapStyleOptions = requireActivity().assets.fileAsString(filename = "google_map_config.json")
        mapView.getMapAsync { map ->
            this.map = map
            viewModel.setMap(map)
            configureMap(map, mapStyleOptions)

            map.setOnCameraIdleListener {
                viewModel.zoomLevelChanged(zoomLevel = map.cameraPosition.zoom)
                viewModel.visibleRegionIdle(map.projection.visibleRegion)
            }
            map.setOnCameraMoveListener { viewModel.visibleRegionChanged(map.projection.visibleRegion) }

            map.moveCamera(initialMapCameraPosition())
            // initial visible region
            viewModel.visibleRegionChanged(map.projection.visibleRegion)


            /**
             * Funneling map click event into the mapClicks Observer so that it could be combined
             * with other Observable stream.
             */
            map.setOnMapClickListener { mapClicks.onNext(true) }

            /**
             * Remove the map object details fragment when user taps outside of object.
             */
            mapClicks
                    .defaultThrottle()
                    .withLatestFromOther(viewModel.displayMode)
                    .subscribe { mapMode ->
                        when (mapMode) {
                            is MapDisplayMode.CurrentFloor -> {
                                val supportFragmentManager = requireActivity().supportFragmentManager
                                supportFragmentManager
                                        .findFragmentByTag(OBJECT_DETAILS)
                                        ?.let {
                                            supportFragmentManager
                                                    .beginTransaction()
                                                    .remove(it)
                                                    .commit()
                                        }
                            }
                            is MapDisplayMode.Tour -> {
                                /*do nothing*/
                            }
                        }

                    }.disposedBy(disposeBag)


            map.setOnMarkerClickListener { marker ->
                val markerTag = marker.tag
                when (markerTag) {
                    is MarkerMetaData<*> -> {
                        when (markerTag.item) {
                            is ArticMapAnnotation -> {
                                val annotation = markerTag.item
                                when (annotation.annotationType) {
                                    ArticMapAnnotationType.DEPARTMENT -> {
                                        viewModel.departmentMarkerSelected(annotation)
                                    }
                                }
                            }
                            is ArticObject -> {
                                val mapObject = markerTag.item
                                viewModel.articObjectSelected(mapObject)
                                map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                            }
                            else -> map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                        }
                    }
                    else -> map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                }
                return@setOnMarkerClickListener true
            }
            groundOverlayGenerated.onNext(true)
        }
    }

    private fun configureMap(map: GoogleMap, mapStyleOptions: String) {
        map.apply {
            isBuildingsEnabled = false
            isIndoorEnabled = false
            isTrafficEnabled = false

            setMapStyle(MapStyleOptions(mapStyleOptions))
            setMinZoomPreference(17f)
            setMaxZoomPreference(22f)

            /** Adding padding to map so that StatusBar doesn't overlap the compass .**/
            setPadding(0, requireActivity().statusBarHeight, 0, 0)

            /**
             * We are setting the bounds here as they are roughly the bounds of the museum,
             * locks us into just that area
             */
            setLatLngBoundsForCameraTarget(museumBounds)
        }

        // TODO: use custom tiling here instead.
        baseGroundOverlay = map.addGroundOverlay(
                GroundOverlayOptions()
                        .positionFromBounds(
                                LatLngBounds(
                                        LatLng(41.874620, -87.629243),
                                        LatLng(41.884753, -87.615841)
                                )
                        ).image(deriveBaseOverlayDescriptor(requireActivity()))
                        .zIndex(0.1f)
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

    override fun setupBindings(viewModel: MapViewModel2) {
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
                .filterFlatMap({ it is MapDisplayMode.Tour }, { it as MapDisplayMode.Tour })
                .subscribe { (tour) ->
                    val fragmentManager = requireActivity().supportFragmentManager
                    fragmentManager.beginTransaction()
                            .replace(R.id.infocontainer, TourCarouselFragment.create(tour), OBJECT_DETAILS)
                            .commit()
                }.disposedBy(disposeBag)


        viewModel.cameraMovementRequested
                .filterValue()
                .subscribe { (newPosition, focus) ->
                    map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(newPosition, focus.toZoomLevel())
                    )
                }.disposedBy(disposeBag)

        viewModel.distinctFloor
                .subscribeBy { floor: Int ->
                    fun backgroundForState(whichFloor: Int): Int {
                        return when (floor) {
                            whichFloor -> R.drawable.map_floor_background_selected
                            else -> R.drawable.map_floor_background_default
                        }
                    }

                    lowerLevel.setBackgroundResource(backgroundForState(0))
                    floorOne.setBackgroundResource(backgroundForState(1))
                    floorTwo.setBackgroundResource(backgroundForState(2))
                    floorThree.setBackgroundResource(backgroundForState(3))
                }
                .disposedBy(disposeBag)

        viewModel.distinctFloor
                .withLatestFrom(groundOverlayGenerated)
                .filter { (floor, generated) -> generated && floor in 0..3 }
                .subscribe { (floor) ->
                    buildingGroundOverlay.setImage(BitmapDescriptorFactory.fromAsset("AIC_Floor$floor.png"))
                }
                .disposedBy(disposeBag)

        viewModel.selectedArticObject
                .withLatestFrom(viewModel.displayMode) { articObject, mapMode ->
                    mapMode to articObject
                }.subscribe { mapModeWithObject ->
                    val selectedArticObject = mapModeWithObject.second
                    val mapMode = mapModeWithObject.first

                    when (mapMode) {
                        is MapDisplayMode.CurrentFloor -> {
                            /**
                             * Display the selected object details.
                             */
                            val fragmentManager = requireActivity().supportFragmentManager
                            fragmentManager.beginTransaction()
                                    .replace(R.id.infocontainer, MapObjectDetailsFragment.create(selectedArticObject), OBJECT_DETAILS)
                                    .commit()
                        }
                        is MapDisplayMode.Tour -> {
                            /* do nothing */
                        }
                    }
                }.disposedBy(disposeBag)

        /**
         * Center the full object marker in the map.
         * When we are in [DisplayMode.Tour] and if the item is being centered, always reset the zoom level to MapZoomLevel.Three
         */
        viewModel
                .selectedTourStopMarkerId
                .flatMap { viewModel.retrieveObjectById(it) }
                .filterValue()
                .subscribe { (_, _, marker) ->
                    val currentZoomLevel = map.cameraPosition.zoom
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, Math.max(ZOOM_INDIVIDUAL, currentZoomLevel)))
                }
                .disposedBy(disposeBag)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()

        val localTour = tour
        if (localTour != null) {
            viewModel.displayMode.onNext(MapDisplayMode.Tour(localTour))
        } else {
            // TODO: Search mode
            viewModel.displayMode.onNext(MapDisplayMode.CurrentFloor)
        }
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

    companion object {
        const val OBJECT_DETAILS = "object-details"
    }
}