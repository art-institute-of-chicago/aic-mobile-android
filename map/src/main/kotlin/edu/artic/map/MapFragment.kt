package edu.artic.map

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.annotation.AnyThread
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
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
import edu.artic.base.utils.isResourceConstrained
import edu.artic.base.utils.loadBitmap
import edu.artic.base.utils.statusBarHeight
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapAnnotationType
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.map.carousel.TourCarouselFragment
import edu.artic.map.rendering.MapItemRenderer
import edu.artic.map.rendering.MarkerMetaData
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.ui.getAudioServiceObservable
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

    private lateinit var baseGroundOverlay: GroundOverlay
    private lateinit var buildingGroundOverlay: GroundOverlay
    private var groundOverlayGenerated: Subject<Boolean> = BehaviorSubject.createDefault(false)
    private var mapClicks: Subject<Boolean> = PublishSubject.create()
    private var leaveTourDialog: AlertDialog? = null

    private var searchObject: ArticObject?
        get() = requireActivity().intent?.extras?.getParcelable(MapActivity.ARG_SEARCH_OBJECT)
        set(value) {
            requireActivity().intent?.putExtra(MapActivity.ARG_SEARCH_OBJECT, value)
        }

    private var searchAmenityType: String?
        get() = requireActivity().intent?.extras?.getString(MapActivity.ARG_SEARCH_AMENITY_TYPE)
        set(value) {
            requireActivity().intent?.putExtra(MapActivity.ARG_SEARCH_AMENITY_TYPE, value)
        }

    private var tour: ArticTour?
        get() = requireActivity().intent?.extras?.getParcelable(MapActivity.ARG_TOUR)
        set(value) {
            requireActivity().intent?.putExtra(MapActivity.ARG_TOUR, value)
        }

    private var startTourStop: ArticTour.TourStop?
        get() = requireActivity().intent?.extras?.getParcelable(MapActivity.ARG_TOUR_START_STOP)
        set(value) {
            requireActivity().intent?.putExtra(MapActivity.ARG_TOUR_START_STOP, value)
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

        /**
         * If displayMode is not [MapDisplayMode.Tour], hide the carousel.
         */
        viewModel.displayMode
                .filter { it !is MapDisplayMode.Tour }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { hideFragmentInInfoContainer() }
                .disposedBy(disposeBag)

        viewModel.tourBoundsChanged
                .withLatestFrom(viewModel.currentMap.filterValue())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (bounds, map) ->
                    map.moveCamera(CameraUpdateFactory
                            .newLatLngBounds(LatLngBounds.builder()
                                    .includeAll(bounds)
                                    .build(), 0))
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
                .subscribeBy { (floor) ->
                    buildingGroundOverlay.setImage(BitmapDescriptorFactory.fromAsset("AIC_Floor$floor.png"))
                }
                .disposedBy(disposeBag)

        viewModel.selectedArticObject
                .withLatestFrom(viewModel.displayMode) { selected, mapMode -> selected to mapMode }
                .filterFlatMap({ (_, mapMode) -> mapMode !is MapDisplayMode.Tour }, { (selected) -> selected })
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

        viewModel
                .leaveTourRequest
                .distinctUntilChanged()
                .subscribe {
                    displayLeaveTourConfirmation()
                }.disposedBy(disposeBag)

        viewModel
                .switchTourRequest
                .distinctUntilChanged()
                .subscribeBy { (currentTour, newTour) ->
                    displaySwitchTourConfirmation(currentTour, newTour)
                }.disposedBy(disposeBag)
    }

    /**
     * Shows contextual information below the map. Also, it adjusts padding on the map to stay
     * in line with product requirements.
     */
    private fun displayFragmentInInfoContainer(fragment: Fragment) {
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.beginTransaction()
                .replace(R.id.infocontainer, fragment, OBJECT_DETAILS)
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
    private fun hideFragmentInInfoContainer() {
        val supportFragmentManager = requireActivity().supportFragmentManager
        supportFragmentManager
                .findFragmentByTag(OBJECT_DETAILS)
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

    private fun displaySwitchTourConfirmation(currentTour: ArticTour, newTour: ArticTour) {
        leaveTourDialog?.dismiss()
        leaveTourDialog = AlertDialog.Builder(requireContext(), R.style.LeaveTourDialogTheme)
                .setMessage(getString(R.string.leaveTour))
                .setPositiveButton(getString(R.string.stay)) { dialog, _ ->
                    tour = currentTour
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.leave)) { dialog, _ ->
                    tour = newTour
                    viewModel.displayModeChanged(MapDisplayMode.Tour(newTour, startTourStop))
                    dialog.dismiss()
                }
                .create()
        leaveTourDialog?.show()

    }

    private fun displayLeaveTourConfirmation() {
        if (leaveTourDialog?.isShowing != true) {
            leaveTourDialog = AlertDialog.Builder(requireContext(), R.style.LeaveTourDialogTheme)
                    .setMessage(getString(R.string.leaveTour))
                    .setPositiveButton(getString(R.string.stay)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.leave)) { dialog, _ ->
                        tour = null
                        viewModel.leaveTour()
                        dialog.dismiss()
                        audioService.subscribe {
                            it.stopPlayer()
                        }.disposedBy(disposeBag)
                    }
                    .create()
            leaveTourDialog?.show()
        }
    }


    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        viewModel.loadMapDisplayMode(tour, startTourStop)
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