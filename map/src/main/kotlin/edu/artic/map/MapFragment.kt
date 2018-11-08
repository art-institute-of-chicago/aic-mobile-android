package edu.artic.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.fuzz.rx.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.globalLayouts
import com.jakewharton.rxbinding2.view.visibility
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.*
import edu.artic.db.models.*
import edu.artic.location.LocationPromptFragment
import edu.artic.location.centerOfMuseumOnMap
import edu.artic.location.mapDisplayBounds
import edu.artic.map.carousel.LeaveCurrentTourDialogFragment
import edu.artic.map.carousel.TourCarouselFragment
import edu.artic.map.helpers.toLatLng
import edu.artic.map.rendering.*
import edu.artic.map.tutorial.TutorialFragment
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.ui.getAudioServiceObservable
import edu.artic.navigation.NavigationConstants
import edu.artic.navigation.NavigationConstants.Companion.ARG_AMENITY_TYPE
import edu.artic.navigation.NavigationConstants.Companion.ARG_EXHIBITION_OBJECT
import edu.artic.navigation.NavigationConstants.Companion.ARG_SEARCH_OBJECT
import edu.artic.navigation.NavigationConstants.Companion.ARG_TOUR
import edu.artic.navigation.NavigationConstants.Companion.ARG_TOUR_START_STOP
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
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

    override val title = R.string.noTitle

    override val layoutResId: Int
        get() = R.layout.fragment_map
    override val screenName: ScreenName
        get() = ScreenName.Map

    override fun hasTransparentStatusBar() = true

    private var tileOverlay: TileOverlay? = null
    private var mapClicks: Subject<Boolean> = PublishSubject.create()
    private var leaveTourDialog: LeaveCurrentTourDialogFragment? = null

    private fun getLatestSearchObject(): ArticSearchArtworkObject? {
        return requireActivity().intent.removeAndReturnParcelable(ARG_SEARCH_OBJECT)
    }

    private fun getLatestSearchObjectType(): String? {
        return requireActivity().intent.removeAndReturnString(ARG_AMENITY_TYPE)
    }

    private fun getLatestExhibitionObject(): ArticExhibition? {
        return requireActivity().intent.removeAndReturnParcelable(ARG_EXHIBITION_OBJECT)
    }

    private fun getLatestTourObject(): ArticTour? {
        return requireActivity().intent.removeAndReturnParcelable(ARG_TOUR)
    }

    private fun getStartTourStop(): ArticTour.TourStop? {
        return requireActivity().intent.removeAndReturnParcelable(ARG_TOUR_START_STOP)
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
            map.setOnCameraMoveListener {
                dismissFirstRunHeader()
                viewModel.visibleRegionChanged(map.projection.visibleRegion)
            }

            map.moveCamera(initialMapCameraPosition())
            //Initial Camera position doesn't load to the actual map position so re-center to center of musuem
            map.moveCamera(CameraUpdateFactory.newLatLng(centerOfMuseumOnMap))
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
                            //TODO remove once ArticObject is verified to not be used anymore
                            is ArticObject -> {
                                val mapObject = markerTag.item
                                viewModel.articObjectSelected(mapObject)
                                map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                            }
                            is MapItemModel -> {
                                val backing = markerTag.item.backingObject
                                backing?.let {
                                    viewModel.articObjectSelected(backing)
                                    map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                                }
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

        val infoAdapter = object : GoogleMap.InfoWindowAdapter {

            override fun getInfoContents(marker: Marker?): View? {
                return context?.let { ctx ->
                    View(ctx)
                }
            }

            override fun getInfoWindow(marker: Marker?): View? {
                /**
                 * Returning empty textView or view didn't work so added TextView with spaces.
                 */
                return context?.let { ctx ->
                    TextView(ctx).apply {
                        text = " "
                    }
                }
            }
        }

        map.apply {
            isBuildingsEnabled = false
            isIndoorEnabled = false
            isTrafficEnabled = false
            this.uiSettings.isMyLocationButtonEnabled = false
            this.uiSettings.isCompassEnabled = false
            enableLocation()
            setMapStyle(MapStyleOptions(mapStyleOptions))
            setMinZoomPreference(ZOOM_MIN)
            setMaxZoomPreference(ZOOM_MAX)

            /** Adding padding to map so that StatusBar doesn't overlap the compass .**/
            setPadding(0, requireActivity().statusBarHeight, 0, 0)

            /**
             * We are setting the bounds here as they are roughly the bounds of the museum,
             * locks us into just that area
             */
            setLatLngBoundsForCameraTarget(mapDisplayBounds)

            /**
             * Marker.showInfoWindow() method is used (see [MapViewModel.retrieveMarkerByObjectId]
             * to bring the selected marker to front (markers sometime overlap).
             * We are using this method just to bring the targeted marker to front without
             * displaying info window.
             *
             * Purpose of setting this infoWindowAdapter is for displaying an empty info window.
             */
            setInfoWindowAdapter(infoAdapter)
        }
    }

    /**
     * # Request that this map track the user's location.
     * If the fine location permission was not granted, it does nothing.
     *
     * ## NB:
     * _This is a separate method because `lint`'s `MissingPermission` check cannot
     * detect permission checks proxied through non-support-library functions like
     * [hasFineLocationPermission]. Hence, we suppress that check on this function alone
     * to prevent spurious `lint` failures._
     */
    @SuppressLint("MissingPermission")
    private fun GoogleMap.enableLocation() {
        if (requireActivity().hasFineLocationPermission()) {
            this.isMyLocationEnabled = true
        }
    }

    fun dismissFirstRunHeader() {
        if (mapFirstRunHeaderFrame.visibility == View.VISIBLE) {
            viewModel.onTouchWithHeader()
        }
    }

    @SuppressLint("MissingPermission")
    override fun setupBindings(viewModel: MapViewModel) {

        getAudioServiceObservable()
                .bindTo(audioService)
                .disposedBy(disposeBag)

        lowerLevel.clicks()
                .subscribe {
                    dismissFirstRunHeader()
                    viewModel.floorChangedTo(0)
                }
                .disposedBy(disposeBag)

        floorOne.clicks()
                .subscribe {
                    dismissFirstRunHeader()
                    viewModel.floorChangedTo(1)
                }
                .disposedBy(disposeBag)

        floorTwo.clicks()
                .subscribe {
                    dismissFirstRunHeader()
                    viewModel.floorChangedTo(2)
                }
                .disposedBy(disposeBag)

        floorThree.clicks()
                .subscribe {
                    dismissFirstRunHeader()
                    viewModel.floorChangedTo(3)
                }
                .disposedBy(disposeBag)

        compass.clicks()
                .subscribeBy {
                    dismissFirstRunHeader()
                    viewModel.onClickCompass()
                }.disposedBy(disposeBag)

        searchIcon.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onClickSearch()
                }.disposedBy(disposeBag)

        mapFirstRunHeaderFrame.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                viewModel.onTouchWithHeader()
            }
            return@setOnTouchListener false
        }

        viewModel.showFirstRunHeader
                .bindToMain(mapFirstRunHeaderFrame.visibility())
                .disposedBy(disposeBag)

        viewModel.chosenInfo
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    mapFirstRunHeaderTitle.text = it.mapTitle
                    mapFirstRunHeaderSubtitle.text = it.mapSubtitle
                }
                .disposedBy(disposeBag)

        viewModel.displayMode
                .filterTo<MapDisplayMode, MapDisplayMode.Tour>()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (tour) -> displayFragmentInInfoContainer(TourCarouselFragment.create(tour)) }
                .disposedBy(disposeBag)

        viewModel.displayMode
                .filterTo<MapDisplayMode, MapDisplayMode.Search<*>>()
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
                        is MapDisplayMode.Search.ExhibitionSearch ->
                            displayFragmentInInfoContainer(
                                    SearchObjectDetailsFragment.loadExhibitionResults(searchObject.item),
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
                                CameraUpdateFactory.newLatLngZoom(
                                        centerOfMuseumOnMap,
                                        ZOOM_INITIAL
                                )
                        )
                    } else {
                        // Make sure we can see all of the specified positions
                        val aoiBounds = LatLngBounds.builder().includeAll(bounds).build()
                        map.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                        aoiBounds.center,
                                        Math.max(ZOOM_INDIVIDUAL, map.cameraPosition.zoom)
                                )
                        )
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
                .observeOn(AndroidSchedulers.mainThread())
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

                    // We want to add the new `TileOverlay` before removing the old one
                    val nextFloorPlan = map.addTileOverlay(TileOverlayOptions()
                            .zIndex(0.2f)
                            .fadeIn(false)
                            .transparency(TRANSPARENCY_INVISIBLE)
                            .tileProvider(GlideMapTileProvider(requireContext(), floor)))

                    nextFloorPlan.graduallyFadeIn()

                    tileOverlay?.removeWithFadeOut()
                    tileOverlay = nextFloorPlan
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
         *
         * When we are in [MapDisplayMode.Tour] and the item is being centered, we always
         * need to reset the zoom level to [ZOOM_INDIVIDUAL] - we subscribe to
         * [MapViewModel.selectedTourStopMarkerId] here to detect that scenario.
         *
         * Other [MapDisplayMode]s share the [MapViewModel.selectedArticObject] field, so
         * a single subscription on [MapViewModel.boundsOfInterestChanged] higher-up in
         * this function handles all that.
         */
        viewModel
                .selectedTourStopMarkerId
                .flatMap { viewModel.retrieveMarkerByObjectId(it) }
                .filterValue()
                .withLatestFrom(viewModel.currentMap.filterValue())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (marker, map) ->

                    /**
                     * showInfoWindow() is only used for bringing targeted marker to front.
                     */
                    marker.showInfoWindow()
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (_, _) ->
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

        Observables.combineLatest(
                viewModel.isUserInMuseum,
                viewModel.currentMap
        ).filter { (_, currentMap) -> currentMap.value != null }
                .subscribe { (inMuseum, currentMap) ->
                    val map = currentMap.value!!
                    if (map.isMyLocationEnabled != inMuseum) {
                        map.isMyLocationEnabled = inMuseum
                    }
                }.disposedBy(disposeBag)

        //TODO re-enable once we figure out why this feature isn't working great on site
//        viewModel.showCompass
//                .bindToMain(compass.visibility())
//                .disposedBy(disposeBag)

        viewModel.focusToTracking
                .distinctUntilChanged()
                .subscribe { (map, wrapped) ->
                    val location = wrapped.value
                    if (location != null) {
                        compass.rotation = 0.0f
                        compass.alpha = 1.0f
                        map.uiSettings.isRotateGesturesEnabled = false
                        map.uiSettings.isScrollGesturesEnabled = false

                        map.animateCamera(
                                CameraUpdateFactory
                                        .newCameraPosition(
                                                CameraPosition.Builder(map.cameraPosition)
                                                        .bearing(location.bearing)
                                                        .target(LatLng(location.latitude, location.longitude))
                                                        .build()
                                        )
                        )
                    } else {
                        map.uiSettings.isRotateGesturesEnabled = true
                        map.uiSettings.isScrollGesturesEnabled = true
                        compass.rotation = 30f
                        compass.alpha = .5f
                    }
                }
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: MapViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .filterFlatMap({ it is Navigate.Forward }, { (it as Navigate.Forward).endpoint })
                .subscribe {

                    val act = requireActivity()
                    val manager: FragmentManager = act.supportFragmentManager ?: return@subscribe

                    when (it) {
                        MapViewModel.NavigationEndpoint.Search -> {
                            val intent = NavigationConstants.SEARCH.asDeepLinkIntent()
                            startActivity(intent)
                        }
                        MapViewModel.NavigationEndpoint.LocationPrompt -> {
                            /**
                             * Display location prompt iff location permission is not granted.
                             */
                            if (!act.hasFineLocationPermission()) {
                                manager
                                        .beginTransaction()
                                        .replace(R.id.overlayContainer, LocationPromptFragment(), "LocationPromptFragment")
                                        .addToBackStack("LocationPromptFragment")
                                        .commit()
                            }
                        }
                        is MapViewModel.NavigationEndpoint.Tutorial -> {
                            manager
                                    .beginTransaction()
                                    .replace(R.id.overlayContainer, TutorialFragment.withExtras(it.currentFloor), "TutorialFragment")
                                    .addToBackStack("TutorialFragment")
                                    .commit()
                        }
                    }
                }.disposedBy(navigationDisposeBag)

        viewModel.setupPreferenceBindings(navigationDisposeBag)
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
        viewModel.loadMapDisplayMode(requestedTour to requestedStop)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        viewModel.onResume(getLatestTourObject() to getStartTourStop(),
                getLatestSearchObject(),
                getLatestSearchObjectType(),
                getLatestExhibitionObject())
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
        leaveTourDialog?.dismissAllowingStateLoss()


        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
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