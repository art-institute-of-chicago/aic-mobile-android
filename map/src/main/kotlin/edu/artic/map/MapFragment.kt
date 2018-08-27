package edu.artic.map

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.annotation.AnyThread
import android.support.annotation.GuardedBy
import android.support.annotation.UiThread
import android.support.annotation.WorkerThread
import android.util.Log
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
import edu.artic.base.utils.*
import edu.artic.db.models.ArticMapAmenityType
import edu.artic.db.models.ArticMapAnnotationType
import edu.artic.db.models.ArticTour
import edu.artic.image.loadWithThumbnail
import edu.artic.map.carousel.TourCarouselFragment
import edu.artic.map.helpers.*
import edu.artic.map.util.*
import edu.artic.ui.util.asCDNUri
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_map.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

typealias BitmapTarget = com.bumptech.glide.request.target.Target<Bitmap>

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

    /**
     * Synchronization lock for [fullObjectMarkers].
     */
    private val fullObjectsLock = ReentrantLock()

    private var leaveTourDialog: AlertDialog? = null
    private val amenitiesMarkerList = mutableListOf<Marker>()
    private val spaceOrLandmarkMarkerList = mutableListOf<Marker>()
    private val departmentMarkers = mutableListOf<Marker>()
    private val galleryMarkers = mutableListOf<Marker>()
    /**
     * This is a list of all the [MapItem.Object] elements (and if present,
     * the one [MapItem.TourIntro]) that are currently shown on the map.
     *
     * This list is updated against [MapViewModel.whatToDisplayOnMap]
     * as the visible set of [MapItem]s changes. Accessing
     * [Marker.getTag] is only permitted on the UI thread, and may
     * incur significant delays (3-4 seconds for 200 accesses). For
     * that reason, we cache the tag here in a [BiasedPair].
     *
     * Please lock all usage of this field against concurrency with
     * [fullObjectsLock].
     */
    @GuardedBy("fullObjectsLock")
    private val fullObjectMarkers = mutableListOf<BiasedPair<MapItem<*>, Marker>>()

    /**
     * Cache of network response targets; this helps us avoid making too
     * many API calls for the same image resource.
     */
    private val targetCache: MutableMap<MapItem<*>, BitmapTarget> = mutableMapOf()

    private lateinit var objectMarkerGenerator: ArticObjectMarkerGenerator
    private lateinit var galleryNumberGenerator: GalleryNumberMarkerGenerator
    private lateinit var departmentMarkerGenerator: DepartmentMarkerGenerator

    private lateinit var baseGroundOverlay: GroundOverlay
    private lateinit var buildingGroundOverlay: GroundOverlay
    private var groundOverlayGenerated: Subject<Boolean> = BehaviorSubject.createDefault(false)
    private val mapClicks: Subject<Boolean> = PublishSubject.create()
    private val mapMovements: Subject<Boolean> = PublishSubject.create()
    /**
     * The region of [map] that was visible during the last 'camera idle' event.
     */
    private val visibleArea: Subject<VisibleRegion> = BehaviorSubject.create()


    companion object {
        const val OBJECT_DETAILS = "object-details"
        const val ZOOM_LEVEL_ONE = 18.0f
        const val ZOOM_LEVEL_TWO = 19.0f
        const val ZOOM_LEVEL_THREE = 20.0f
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        objectMarkerGenerator = ArticObjectMarkerGenerator(view.context)
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

            map.setOnCameraMoveListener {
                mapMovements.onNext(true)
                // This event catches the end of any sequence of movement events, so the below
                // line makes sure `visibleArea` contains the most accurate value when idle.
                visibleArea.onNext(map.projection.visibleRegion)
            }

            map.moveCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                            .target(LatLng(41.879592, -87.622491))
                            .bearing(90f)
                            .tilt(45f)
                            .build()
            ))

            mapMovements
                    // Up to ten events a second.
                    .throttleFirst(100, TimeUnit.MILLISECONDS)
                    .subscribe {
                        visibleArea.onNext(map.projection.visibleRegion)
                    }.disposedBy(disposeBag)


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
                val tag = marker.tag

                when (tag) {
                    is MapItem.Annotation -> {
                        when (tag.item.annotationType) {
                            ArticMapAnnotationType.DEPARTMENT -> {
                                viewModel.departmentMarkerSelected(tag.item)
                            }
                        }
                    }
                    is MapItem.Object -> {
                        val mapObject = tag.item
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
                .distinctUntilChanged()
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
                                        MapZoomLevel.One -> ZOOM_LEVEL_ONE
                                        MapZoomLevel.Two -> ZOOM_LEVEL_TWO
                                        MapZoomLevel.Three -> ZOOM_LEVEL_THREE
                                    }
                            )
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

        Observables.combineLatest(
                viewModel.distinctFloor,
                groundOverlayGenerated.filter { it }
        ) { floor, _ ->
            floor
        }.filter { floor -> floor in 0..3 }
                .subscribe {
                    buildingGroundOverlay.setImage(BitmapDescriptorFactory.fromAsset("AIC_Floor$it.png"))
                }.disposedBy(disposeBag)

        viewModel.amenities
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { annotationList ->
                    loadMarkersForAnnotation(
                            annotationList,
                            amenitiesMarkerList
                    ) { mapItem ->
                        val icon = amenityIconForAmenityType(mapItem.item.amenityType)
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

                    departmentMarkers.modifyThenRemoveIf { marker ->
                        val shouldRemove = itemList.doesNotContain(marker.tag)
                        if (shouldRemove) {
                            marker.removeWithFadeOut()
                        }
                        return@modifyThenRemoveIf shouldRemove
                    }
                    galleryMarkers.modifyThenRemoveIf { marker ->
                        val shouldRemove = itemList.doesNotContain(marker.tag)
                        if (shouldRemove) {
                            marker.removeWithFadeOut()
                        }
                        return@modifyThenRemoveIf shouldRemove
                    }
                    fullObjectsLock.withLock {
                        fullObjectMarkers.modifyThenRemoveIf { (model, marker) ->

                            val shouldRemove = itemList.doesNotContain(model)
                            if (shouldRemove) {
                                if (marker.tryExpectingFailure(true, Marker::removeWithFadeOut)) {
                                    if (BuildConfig.DEBUG && model is MapItem.Object) {
                                        Log.e("MapMarker", "Removal failed: " + model.item.nid)
                                    }
                                }
                                val target = targetCache.remove(model)
                                Glide.with(this).clear(target)
                            }
                            return@modifyThenRemoveIf shouldRemove
                        }
                    }


                    Schedulers.io().scheduleDirect {
                        bindMarkersAsynchronously(itemList, mapMode)
                    }.disposedBy(disposeBag)

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
                    fullObjectsLock.withLock {
                        fullObjectMarkers.firstOrNull { (model, _) ->
                            model is MapItem.Object && model.item.nid == nid
                                    ||
                                    model is MapItem.TourIntro && model.item.nid == nid
                        }?.let { (_, marker) ->
                            val currentZoomLevel = map.cameraPosition.zoom
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, Math.max(ZOOM_LEVEL_THREE, currentZoomLevel)))
                        }
                    }

                }.disposedBy(disposeBag)

        viewModel
                .leaveTourRequest
                .subscribe {
                    displayLeaveTourConfirmation(viewModel)
                }.disposedBy(disposeBag)

        viewModel
                .displayMode
                .distinctUntilChanged()
                .filter { mode -> mode is MapViewModel.DisplayMode.CurrentFloor }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val supportFragmentManager = requireActivity().supportFragmentManager
                    val fragment = supportFragmentManager.findFragmentByTag(OBJECT_DETAILS)
                    fragment?.let { carousalFragment ->
                        supportFragmentManager
                                .beginTransaction()
                                .remove(carousalFragment)
                                .commit()
                    }
                }
                .disposedBy(disposeBag)
    }

    private fun displayLeaveTourConfirmation(viewModel: MapViewModel) {
        if (leaveTourDialog?.isShowing != true) {
            leaveTourDialog = AlertDialog.Builder(requireContext(), R.style.LeaveTourDialogTheme)
                    .setMessage(getString(R.string.leaveTour))
                    .setPositiveButton(getString(R.string.stay)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.leave)) { dialog, _ ->
                        viewModel.leaveTour()
                        dialog.dismiss()
                    }
                    .create()
            leaveTourDialog?.show()
        }
    }

    @UiThread
    fun bindToMap(mapItem: MapItem<*>, mapMode: MapViewModel.DisplayMode) {
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
                loadGalleryNumber(mapItem)
            }
            is MapItem.Object -> {
                loadObject(mapItem, mapMode)
            }
            is MapItem.TourIntro -> {
                loadTourObject(mapItem, mapMode)
            }

        }
    }


    @WorkerThread
    fun bindMarkersAsynchronously(itemList: List<MapItem<*>>, mapMode: MapViewModel.DisplayMode) {
        val coreCount = Runtime.getRuntime().availableProcessors()

        val batchedItems : List<List<MapItem<*>>>
        batchedItems = if (requireContext().isResourceConstrained()) {
            // We don't have that many resources. Only pass along 'number of cores' items at a time
            itemList.chunked(coreCount)
        } else {
            // Work with 3 x 'number of processor cores' items at a time.
            itemList.chunked(3 * coreCount)
        }

                Observables.zip(
                        // There's an Observable operator 'window', but that's (severely) restricted to a single subscriber
                        batchedItems.toObservable(),
                        // The 'interval' here will make sure only one event is emitted every 150 ms
                        Observable.interval(150, TimeUnit.MILLISECONDS)
                )
                .map {
                    val mapItems = it.first

                    return@map mapItems.filter(this@MapFragment::alreadyHasMarker)
                }.filter {
                    it.isNotEmpty()
                }.subscribeBy {
                    it.toObservable()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeBy { mapItem ->
                                bindToMap(mapItem, mapMode)
                            }.disposedBy(disposeBag)
                }.disposedBy(disposeBag)
    }

    /**
     * If there is already a marker for [mapItem] in 'fullObjectMarkers', we
     * don't need to start another load. This method helps us detect that case.
     *
     * Currently, we only look for duplicates of [MapItem.Object]s;
     * other types of items may be filtered in a future commit.
     */
    private fun alreadyHasMarker(mapItem: MapItem<*>): Boolean {
        return (mapItem !is MapItem.Object
                || fullObjectsLock.withLock {
                    fullObjectMarkers.all { (model, _) ->
                        model != mapItem
                    }
                }
        )
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

    private fun loadGalleryNumber(mapGallery: MapItem.Gallery) {
        val gallery = mapGallery.item

        gallery.number?.let {
            val marker = map.addMarker(MarkerOptions()
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
            marker.tag = mapGallery
            galleryMarkers.add(marker)
        }
    }

    private fun loadDepartment(annotation: MapItem.Annotation) {
        val department = annotation.item
        val floor = annotation.floor
        Glide.with(this)
                .asBitmap()
                .load(department.imageUrl?.asCDNUri())
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

    private fun loadObject(mapObject: MapItem.Object, displayMode: MapViewModel.DisplayMode) {


        val floor = mapObject.floor
        val articObject = mapObject.item


        // We'll need a marker no matter what....
        val objectPosition = articObject.toLatLng()

        // Get a placeholder onto the map ASAP
        val fullMarker: Marker
        fullMarker = map.addMarker(
                MarkerOptions()
                        .position(objectPosition)
                        .icon(BitmapDescriptorFactory.fromBitmap(
                                objectMarkerGenerator.makeIcon(null, .15f)
                        ))
                        .zIndex(2f)
                        .visible(true)

        )

        fullMarker.tag = mapObject

        /* If the tour is not in the current floor make the ui translucent*/
        fullMarker.alpha = getAlphaValue(displayMode, floor)

        val paired: BiasedPair<MapItem<*>, Marker> = BiasedPair(mapObject, fullMarker)

        fullObjectsLock.withLock {
            fullObjectMarkers.add(paired)
        }

        val isDot: AtomicBoolean = AtomicBoolean(true)


        // All of this detecting changes logic is solely relevant for 'ArticObject's at this point in time.
        visibleArea.observeOn(Schedulers.computation())
                .takeUntil {
                    fullObjectsLock.withLock {
                        fullObjectMarkers.doesNotContain(paired)
                    }
                }.observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {region ->

                    if (objectPosition.isCloseEnoughToCenter(region.latLngBounds)) {
                        if (isDot.compareAndSet(true, false)) {
                            // First switch to 'loading' icon
                            fullMarker.tryExpectingFailure {
                                it.setIcon(
                                        BitmapDescriptorFactory.fromBitmap(
                                                objectMarkerGenerator.makeIcon(null, .7f)
                                        )
                                )
                            }
                            // Now we must make the call - show the image at its full size
                            Glide.with(this)
                                    .asBitmap()
                                    // The 'objectMarkerGenerator' used by the below target only supports bitmaps rendered in software
                                    .apply(RequestOptions().disallowHardwareConfig())
                                    .loadWithThumbnail(
                                            articObject.thumbnailFullPath?.asCDNUri(),
                                            // Prefer 'image_url', fall back to 'large image' if necessary.
                                            (articObject.image_url ?: articObject.largeImageFullPath)?.asCDNUri()
                                    )
                                    .into(getTargetFor(mapObject, fullMarker, displayMode))
                            isDot.set(false)
                        }
                    } else if (isDot.compareAndSet(false, true)) {
                        // Show as small dot
                        fullMarker.tryExpectingFailure {
                            it.setIcon(BitmapDescriptorFactory.fromBitmap(
                                    objectMarkerGenerator.makeIcon(null, .15f)
                            ))
                        }
                        isDot.set(true)
                    }
                }.disposedBy(disposeBag)




    }

    private fun getTargetFor(mapObject: MapItem.Object, fullMarker: Marker, displayMode: MapViewModel.DisplayMode): BitmapTarget {
        val cached = targetCache[mapObject]

        if (cached == null) {
            val floor = mapObject.floor
            val articObject = mapObject.item

            val newTarget: BitmapTarget = object : SimpleTarget<Bitmap>() {
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


                        // Switch to full icon (from placeholder dot)
                        fullMarker.tryExpectingFailure {
                            it.setIcon(
                                    BitmapDescriptorFactory.fromBitmap(
                                            objectMarkerGenerator.makeIcon(resource, 1f, order)
                                    )
                            )
                        }


                    }
                }
            }
            targetCache[mapObject] = newTarget
            return newTarget
        } else {
            return cached
        }
    }

    /**
     * Loads the tour intro object as the marker in the map.
     */
    private fun loadTourObject(mapTour: MapItem.TourIntro, displayMode: MapViewModel.DisplayMode) {
        val articTour = mapTour.item
        val floor = mapTour.floor

        Glide.with(this)
                .asBitmap()
                .load(articTour.thumbnailFullPath?.asCDNUri())
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
                            fullMaker.tag = mapTour
                            fullObjectsLock.withLock {
                                fullObjectMarkers.add(BiasedPair(mapTour, fullMaker))
                            }
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

        if (tour != null) {
            viewModel.loadTourMode(tour)
        }
    }
}


