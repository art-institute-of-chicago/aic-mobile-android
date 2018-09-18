package edu.artic.map

import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.view.View
import com.fuzz.rx.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.globalLayouts
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.fileAsString
import edu.artic.base.utils.statusBarHeight
import edu.artic.db.models.*
import edu.artic.map.carousel.LeaveCurrentTourDialogFragment
import edu.artic.map.carousel.TourCarouselFragment
import edu.artic.map.helpers.toLatLng
import edu.artic.map.rendering.GlideMapTileProvider
import edu.artic.map.rendering.MapItemRenderer
import edu.artic.map.rendering.MarkerMetaData
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.ui.getAudioServiceObservable
import edu.artic.navigation.NavigationConstants.Companion.ARG_AMENITY_TYPE
import edu.artic.navigation.NavigationConstants.Companion.ARG_SEARCH_OBJECT
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
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
 * The rendering is handled buy the [MapMarkerConstructor] which contains all of the [MapItemRenderer]
 * that handle image fetching, backpressure, and buffering.
 *
 * This fragment should remain small as possible and delegate its functionality to [MapViewModel]
 * and other constructs.
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

    private var tileOverlay: TileOverlay? = null
    private var mapClicks: Subject<Boolean> = PublishSubject.create()
    private var leaveTourDialog: LeaveCurrentTourDialogFragment? = null

    private fun getLatestSearchObject(): ArticSearchArtworkObject? {
        val data: ArticSearchArtworkObject? = requireActivity().intent?.getParcelableExtra(ARG_SEARCH_OBJECT)
        requireActivity().intent?.removeExtra(ARG_SEARCH_OBJECT)
        return data
    }

    private fun getLatestSearchObjectType(): String? {
        val data: String? = requireActivity().intent?.getStringExtra(ARG_AMENITY_TYPE)
        requireActivity().intent?.removeExtra(ARG_AMENITY_TYPE)
        return data
    }

    private fun getLatestTourObject(): ArticTour? {
        val tour: ArticTour? = requireActivity().intent?.getParcelableExtra(MapActivity.ARG_TOUR)
        requireActivity().intent?.extras?.remove(MapActivity.ARG_TOUR)
        return tour
    }

    private fun getStartTourStop(): ArticTour.TourStop? {
        val startStop: ArticTour.TourStop? = requireActivity().intent?.getParcelableExtra(MapActivity.ARG_TOUR_START_STOP)
        requireActivity().intent?.extras?.remove(MapActivity.ARG_TOUR_START_STOP)
        return startStop
    }


    private val audioService: Subject<AudioPlayerService> = BehaviorSubject.create()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)
        viewModel.mapMarkerConstructor.associateContext(view.context)

        MapsInitializer.initialize(view.context)
        val mapStyleOptions = requireActivity().assets.fileAsString(filename = "google_map_config.json")
        mapView.getMapAsync { map ->
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
                    .filterTo<MapDisplayMode, MapDisplayMode.CurrentFloor>()
                    .subscribeBy { hideFragmentInInfoContainer() }
                    .disposedBy(disposeBag)

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
        }
    }

    private fun configureMap(map: GoogleMap, mapStyleOptions: String) {
        map.apply {
            isBuildingsEnabled = false
            isIndoorEnabled = false
            isTrafficEnabled = false

            setMapStyle(MapStyleOptions(mapStyleOptions))
            setMinZoomPreference(ZOOM_MIN)
            setMaxZoomPreference(ZOOM_MAX)

            /** Adding padding to map so that StatusBar doesn't overlap the compass .**/
            setPadding(0, requireActivity().statusBarHeight, 0, 0)

            /**
             * We are setting the bounds here as they are roughly the bounds of the museum,
             * locks us into just that area
             */
            setLatLngBoundsForCameraTarget(museumBounds)
        }
    }

    override fun setupBindings(viewModel: MapViewModel) {

        getAudioServiceObservable()
                .bindTo(audioService)
                .disposedBy(disposeBag)

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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (tour) -> displayFragmentInInfoContainer(TourCarouselFragment.create(tour)) }
                .disposedBy(disposeBag)

        viewModel.displayMode
                .filterFlatMap({ it is MapDisplayMode.Search<*> }, { it as MapDisplayMode.Search<*> })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { searchObject ->
                    when (searchObject) {
                        is MapDisplayMode.Search.ObjectSearch -> {
                            displayFragmentInInfoContainer(
                                    SearchObjectDetailsFragment.loadArtworkResults(searchObject.item),
                                    SEARCH_DETAILS
                            )
                        }
                        is MapDisplayMode.Search.AmenitiesSearch ->
                            displayFragmentInInfoContainer(
                                    SearchObjectDetailsFragment.loadAmenitiesByType(searchObject.item),
                                    SEARCH_DETAILS
                            )
                    }
                }
                .disposedBy(disposeBag)

        /**
         * If displayMode is not [MapDisplayMode.Tour], hide the carousel.
         */
        viewModel.displayMode
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    when (it) {
                        is MapDisplayMode.Tour -> hideFragmentInInfoContainer(SEARCH_DETAILS)
                        is MapDisplayMode.CurrentFloor -> hideFragmentInInfoContainer(SEARCH_DETAILS)
                        is MapDisplayMode.Search<*> -> hideFragmentInInfoContainer(OBJECT_DETAILS)
                    }
                }
                .disposedBy(disposeBag)

        viewModel.boundsOfInterestChanged
                .withLatestFrom(viewModel.currentMap.filterValue(), viewModel.displayMode)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (bounds, map, mode) ->

                    if (mode is MapDisplayMode.Search<*>) {
                        // Only need to zoom out all the way. Specific bounds are irrelevant
                        map.animateCamera(
                                CameraUpdateFactory.zoomTo(ZOOM_MIN)
                        )
                    } else {
                        // Make sure we can see all of the specified positions
                        map.moveCamera(CameraUpdateFactory
                                .newLatLngBounds(
                                        LatLngBounds.builder()
                                                .includeAll(bounds)
                                                .build(),
                                        0
                                ))
                    }
                }
                .disposedBy(disposeBag)

        viewModel.individualMapChange
                .filterValue()
                .withLatestFrom(viewModel.currentMap.filterValue())
                .subscribeBy { (change, map) ->
                    val (newPosition, focus) = change
                    map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(newPosition, focus.toZoomLevel())
                    )
                }
                .disposedBy(disposeBag)

        viewModel.distinctFloor
                .subscribeBy { floor: ArticMapFloor ->
                    fun backgroundForState(whichFloor: Int): Int {
                        return when (floor.label.toInt()) {
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
                .observeOn(AndroidSchedulers.mainThread())
                .filter { floor -> floor.number in 0..3 }
                .withLatestFrom(viewModel.currentMap.filterValue())
                .subscribeBy { (floor, map) ->
                    tileOverlay?.remove()
                    tileOverlay = map.addTileOverlay(TileOverlayOptions()
                            .zIndex(0.2f)
                            .tileProvider(GlideMapTileProvider(requireContext(), floor)))
                }
                .disposedBy(disposeBag)

        viewModel.selectedArticObject
                .withLatestFrom(viewModel.displayMode) { selected, mapMode -> selected to mapMode }
                .filterFlatMap({ (_, mapMode) -> mapMode is MapDisplayMode.CurrentFloor }, { (selected) -> selected })
                .subscribeBy { selected ->
                    displayFragmentInInfoContainer(MapObjectDetailsFragment.create(selected))
                }
                .disposedBy(disposeBag)

        /**
         * Center the full object marker in the map.
         * When we are in [DisplayMode.Tour] and if the item is being centered, always reset the zoom level to MapZoomLevel.Three
         */
        viewModel
                .selectedTourStopMarkerId
                .flatMap { viewModel.retrieveObjectById(it) }
                .filterValue()
                .withLatestFrom(viewModel.currentMap.filterValue())
                .subscribeBy { (markerHolder, map) ->
                    val (_, _, marker) = markerHolder
                    val currentZoomLevel = map.cameraPosition.zoom
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position,
                            Math.max(ZOOM_INDIVIDUAL, currentZoomLevel)))
                }
                .disposedBy(disposeBag)

        viewModel.selectedDiningPlace
                .filterValue()
                .withLatestFrom(viewModel.currentMap.filterValue())
                .subscribeBy { (annotation, map) ->
                    val currentZoomLevel = map.cameraPosition.zoom
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(annotation.toLatLng(),
                            Math.max(ZOOM_INDIVIDUAL, currentZoomLevel)))
                }
                .disposedBy(disposeBag)

        viewModel
                .leaveTourRequest
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    displayLeaveTourConfirmation()
                }.disposedBy(disposeBag)

        viewModel
                .switchTourRequest
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (currentTour, startStop) ->
                    displayLeaveTourConfirmation()
                }.disposedBy(disposeBag)

        /**
         * Stop the active tour & dismiss the tour carousel if user leaves tour.
         */
        viewModel.leftActiveTour
                .filter { it }
                .withLatestFrom(audioService)
                .subscribeBy { (_, service) ->
                    service.stopPlayer()
                    hideFragmentInInfoContainer()
                    refreshMapDisplayMode()
                }
                .disposedBy(disposeBag)
    }

    /**
     * Shows contextual information below the map. Also, it adjusts padding on the map to stay
     * in line with product requirements.
     */
    private fun displayFragmentInInfoContainer(fragment: Fragment, tag: String = OBJECT_DETAILS) {
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.beginTransaction()
                .replace(R.id.infocontainer, fragment, tag)
                .commit()

        // wait for it to update first time
        infocontainer.globalLayouts()
                .withLatestFromOther(viewModel.currentMap.filterValue())
                .take(1)
                .subscribe { map ->
                    val height = infocontainer.height
                    map.setMapPadding(activity = requireActivity(), bottom = height)
                }
                .disposedBy(disposeBag)
    }

    /**
     * Removes the fragment information below the map, and readjusts the padding back to normal.
     */
    @UiThread
    private fun hideFragmentInInfoContainer(tag: String = OBJECT_DETAILS) {
        val supportFragmentManager = requireActivity().supportFragmentManager
        supportFragmentManager
                .findFragmentByTag(tag)
                ?.let {
                    supportFragmentManager
                            .beginTransaction()
                            .remove(it)
                            .commit()
                }

        // reset back to initial.
        viewModel.currentMap
                .take(1)
                .filterValue()
                .subscribeBy { map -> map.setMapPadding(requireActivity()) }
                .disposedBy(disposeBag)
    }


    private fun displayLeaveTourConfirmation() {
        leaveTourDialog?.dismiss()
        val fm = requireFragmentManager()
        leaveTourDialog = LeaveCurrentTourDialogFragment().apply {
            attachTourStateListener(object : LeaveCurrentTourDialogFragment.LeaveTourCallback {
                override fun leftTour() {
                    viewModel.leaveCurrentTour()
                }

                override fun stayed() {
                    viewModel.stayWithCurrentTour()
                }

            })
            this.show(fm, "LeaveTour")
        }

    }

    private fun refreshMapDisplayMode(requestedTour: ArticTour? = null, requestedStop: ArticTour.TourStop? = null) {
        viewModel.loadMapDisplayMode(requestedTour, requestedStop)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        viewModel.onResume(getLatestTourObject(), getStartTourStop(), getLatestSearchObject(), getLatestSearchObjectType())
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
        leaveTourDialog?.dismiss()
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
        const val SEARCH_DETAILS = "SEARCH"
    }
}