package edu.artic.map.rendering

import android.content.Context
import android.support.annotation.AnyThread
import android.support.annotation.UiThread
import com.fuzz.rx.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import edu.artic.db.AccessibilityAware
import edu.artic.db.INVALID_FLOOR
import edu.artic.db.debug
import edu.artic.map.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.TrampolineScheduler
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Defines the main base class for rendering markers on the map. It handles synchronization, batching,
 * fetching, and listening to events on the map. Subclass this to add other custom markers.
 */
abstract class MapItemRenderer<T : AccessibilityAware>(
        /**
         * Minor optimization to register or not register the bitmap queue within a subclass.
         */
        protected val useBitmapQueue: Boolean = false
) {

    protected val mapItems: Subject<Map<String, MarkerHolder<T>>> = BehaviorSubject.createDefault(emptyMap())
    private val currentMap: Subject<Optional<GoogleMap>> = BehaviorSubject.createDefault(Optional(null))
    private var currentFloor: Int = INVALID_FLOOR

    protected val imageQueueDisposeBag = DisposeBag()
    private val imageFetcherDisposeBag = DisposeBag()
    private val bitmapQueue: Subject<DelayedMapItemRenderEvent<T>> = PublishSubject.create()

    protected val visibleRegionChanges: Subject<VisibleRegion> = PublishSubject.create()
    protected val mapChangeEvents: Subject<MapChangeEvent> = PublishSubject.create()

    // this should be the inflated view's context
    lateinit var context: Context

    private val tempMarkers: MutableList<MarkerHolder<T>> = mutableListOf()

    init {
        if (useBitmapQueue) {
            bitmapQueue
                    .toFlowable(BackpressureStrategy.BUFFER)
                    .buffer(100, TimeUnit.MILLISECONDS, 20)
                    .filter { it.isNotEmpty() }
                    .observeOn(AndroidSchedulers.mainThread())
                    .withLatestFrom(currentMap.filterValue().toFlowable(BackpressureStrategy.LATEST),
                            mapItems.toFlowable(BackpressureStrategy.LATEST))
                    .subscribe { (newMarkers, map, existingMapItems) ->
                        // concat our observables to not overload the main thread. Delay each one by 50ms.
                        Single.concat(newMarkers
                                .map { item -> newDeferredMarkerCreationSingle(item, existingMapItems, map) })
                                .observeOn(TrampolineScheduler.instance()) // sequential
                                .toList()
                                .toObservable()
                                .observeOn(AndroidSchedulers.mainThread())
                                .withLatestFrom(mapItems)
                                .map { (newMarkersList, existingMarkers) ->
                                    Timber.d("Updating ${existingMarkers.size} with set of ${newMarkers.size} for ${this::class}")
                                    val modifiedMarkers = existingMarkers.toMutableMap()
                                    newMarkersList.forEach { (newMarker) ->
                                        if (newMarker != null) {
                                            synchronized(tempMarkers) {
                                                tempMarkers -= newMarker
                                            }
                                            // double-check in case marker exists already.
                                            modifiedMarkers[newMarker.id]?.marker?.tryExpectingFailure(true, Marker::removeWithFadeOut)
                                            modifiedMarkers[newMarker.id] = newMarker
                                        }
                                    }
                                    modifiedMarkers
                                }
                                .subscribeBy { mapItems.onNext(it) }
                                .disposedBy(imageFetcherDisposeBag)
                    }
                    .disposedBy(imageQueueDisposeBag)
        }
    }

    /**
     * Return what map focus level these [MapItem] display at. [MapDisplayMode] affects this considerably.
     * [MapDisplayMode.Search] will allow any items in the renderer to display at all levels if its searchable and requested.
     * [MapDisplayMode.Tour] only displays certain items like [ArticObject] at all levels.
     * [MapDisplayMode.CurrentFloor] depends on the [MapFocus] for each type.
     */
    abstract fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus>

    /**
     * Return the zIndex of what you want markers displayed to go at.
     */
    abstract val zIndex: Float

    /**
     * Return the specific items that should render based on map floor and [MapDisplayMode].
     */
    abstract fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<T>>

    /**
     * Retrieve location from the item type. Don't call this directly in subclasses.
     */
    abstract fun getLocationFromItem(item: T): LatLng

    /**
     * Use this method directly to get proper adjusted world position based on our map taking over the world.
     */
    fun getAdjustedLocationFromItem(item: T) = getLocationFromItem(item)

    /**
     * Returns the item's id. Since our objects are mostly different without common interfaces, this method exists.
     */
    abstract fun getIdFromItem(item: T): String

    /**
     * Fast implementation that comes from resources, or an already available asset. If null, we delay the
     * load of the marker until [getBitmapFetcher] completes (if available).
     */
    open fun getFastBitmap(item: T, displayMode: MapDisplayMode): BitmapDescriptor? = null

    /**
     * If implemented, we enqueue a new [Observable] that when completes, forwards its data
     * and re-renders an existing [MarkerHolder]
     */
    open fun getBitmapFetcher(item: T, displayMode: MapDisplayMode): Observable<BitmapDescriptor>? = null

    /**
     * Return marker alpha based on map configuration. By default, all markers are [ALPHA_VISIBLE],
     * but depending on mode, this might be [ALPHA_DIMMED]. If for example, we're on tour and displaying
     * a different floor than current.
     */
    @AnyThread
    open fun getMarkerAlpha(floor: Int, mapDisplayMode: MapDisplayMode, item: T): Float = ALPHA_VISIBLE

    fun getMarkerHolderById(id: String): Observable<Optional<MarkerHolder<T>>> =
            mapItems.take(1).map { mapItems -> optionalOf(mapItems[id]) }

    /**
     * Binds this [MapItemRenderer] to changes to the map, based on [MapChangeEvent] and if the [GoogleMap]
     * is active.
     */
    fun bindToMapChanges(map: Observable<GoogleMap>,
                         mapChangeEvents: Flowable<MapChangeEvent>,
                         visibleRegionChanges: Observable<VisibleRegion>,
                         disposeBag: DisposeBag) {
        renderMarkers(map, mapChangeEvents)
                .subscribeBy { markers -> this.mapItems.onNext(markers.associateBy { it.id }) }
                .disposedBy(disposeBag)

        visibleRegionChanges
                .bindTo(this.visibleRegionChanges)
                .disposedBy(disposeBag)

        mapChangeEvents
                .bindTo(this.mapChangeEvents)
                .disposedBy(disposeBag)
    }

    /**
     * Call this when the renderer will no longer display in the UI and free up resources.
     */
    fun dispose() {
        this.currentMap.onNext(Optional(null))
    }

    /**
     * Enqueues a fetch of the [BitmapDescriptor] associated with this [item] (if specified). Runs
     * the operation on a different thread, posting the result on the [bitmapQueue] for processing.
     */
    @Synchronized
    protected fun enqueueBitmapFetch(item: T, mapChangeEvent: MapChangeEvent): Disposable? {
        getBitmapFetcher(item, mapChangeEvent.displayMode)?.let { fetcher ->
            return@let fetcher
                    .map { DelayedMapItemRenderEvent(mapChangeEvent, item, it) }
                    .subscribeBy { bitmapQueue.onNext(it) }
                    .disposedBy(imageFetcherDisposeBag)
        }
        return null
    }

    private fun renderMarkers(mapObservable: Observable<GoogleMap>, floorFocus: Flowable<MapChangeEvent>)
            : Flowable<List<MarkerHolder<T>>> {
        return floorFocus
                .withLatestFrom(mapObservable
                        .doOnNext { currentMap.onNext(optionalOf(it)) }
                        .toFlowable(BackpressureStrategy.LATEST)) { first, second -> first to second }
                .debug("New Map Event")
                .observeOn(Schedulers.io())
                .flatMap { (mapEvent, map) ->
                    val visibleMapFocus = getVisibleMapFocus(mapEvent.displayMode)
                    if (!visibleMapFocus.contains(mapEvent.focus)) {
                        // clear out list if the focus is not within scope of the renderer.
                        MapItemRendererEvent(map, mapEvent, emptyList<T>()).asFlowable()
                    } else {
                        getItems(mapEvent.floor, mapEvent.displayMode).map {
                            Timber.d("Found ${it.size} items for ${this::class}")
                            MapItemRendererEvent(map, mapEvent, it)
                        }
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(TrampolineScheduler.instance()) // execute serially so we don't overlap on other events on the main thread.
                .withLatestFrom(mapItems.toFlowable(BackpressureStrategy.LATEST),
                        visibleRegionChanges.toFlowable(BackpressureStrategy.LATEST))
                .map { (mapItemRendererEvent, existingMapItems, visibleRegion) ->
                    val (_, _, items) = mapItemRendererEvent

                    imageFetcherDisposeBag.clear()
                    val foundItemsMap = removeOldMarkers(existingMapItems, items)

                    // any markers in between loading and done are removed here.
                    flushTempMarkers()

                    // don't emit an empty event (which empty zip list does), rather emit an
                    // empty list here.
                    if (items.isEmpty()) {
                        listOf<MarkerHolder<T>>()
                    } else {
                        return@map mapAndFetchDisplayMarkers(foundItemsMap, mapItemRendererEvent, visibleRegion)
                    }
                }
    }

    /**
     * Maps out any fast [getFastBitmap] markers, will bind those to the [mapItems]. Will enqueue
     * any delayed markers with [enqueueBitmapFetch]. If marker exists on map currently we reuse it
     * and adjust alpha, if needed.
     */
    @UiThread
    private fun mapAndFetchDisplayMarkers(foundItemsMap: Map<String, MarkerHolder<T>>,
                                          mapRenderEvent: MapItemRendererEvent<T>,
                                          visibleRegion: VisibleRegion): List<MarkerHolder<T>> {
        val (map, mapChangeEvent, items) = mapRenderEvent
        val (_, floor, displayMode) = mapChangeEvent
        return items.mapNotNull { item ->
            val id = getIdFromItem(item)
            val existing = foundItemsMap[id]
            // same item, don't re-add to the map.
            if (existing != null) {
                // adjust alpha, especially for items that are on tour.
                existing.marker.alpha = getMarkerAlpha(floor, displayMode, item)
                adjustVisibleMarker(existing, visibleRegion, mapChangeEvent)
                existing
            } else {
                displayMarker(item,
                        mapChangeEvent, visibleRegion, displayMode, map, floor, id)
            }
        }
    }

    /**
     * Perform custom marker changes when visible.
     */
    @UiThread
    protected open fun adjustVisibleMarker(
            existing: MarkerHolder<T>,
            visibleRegion: VisibleRegion,
            mapChangeEvent: MapChangeEvent
    ) = Unit

    @UiThread
    protected open fun displayMarker(item: T,
                                     mapChangeEvent: MapChangeEvent,
                                     visibleRegion: VisibleRegion,
                                     displayMode: MapDisplayMode,
                                     map: GoogleMap,
                                     floor: Int,
                                     id: String): MarkerHolder<T>? {
        val requestDisposable = enqueueBitmapFetch(item = item, mapChangeEvent = mapChangeEvent)

        // fast bitmap returns immediately.
        return getFastBitmap(item, displayMode)?.let { bitmapDescriptor ->
            constructAndAddMarkerHolder(
                    item = item,
                    bitmap = bitmapDescriptor,
                    displayMode = displayMode,
                    map = map,
                    floor = floor,
                    id = id,
                    isSelected = shouldFocusOnCreation(mapChangeEvent.displayMode),
                    // bitmap queue not used, means bitmap is considered loaded
                    loadedBitmap = !useBitmapQueue,
                    requestDisposable = requestDisposable)
        }
    }

    /**
     * This method is called by [displayMarker], later it is used to focus the marker.
     */
    protected open fun shouldFocusOnCreation(displayMode: MapDisplayMode): Boolean {
        return false
    }

    /**
     * Constructs a [Single] that updates the map marker (if present), otherwise creates and adds
     * the marker to the map.
     */
    private fun newDeferredMarkerCreationSingle(
            item: DelayedMapItemRenderEvent<T>,
            existingMapItems: Map<String, MarkerHolder<T>>,
            map: GoogleMap
    ): Single<Optional<MarkerHolder<T>>>? {
        return Single.fromCallable {
            val (_, floor, displayMode) = item.mapChangeEvent
            val id = getIdFromItem(item.item)
            val existingMarker = existingMapItems[id]
            // reusing existing marker if possible.
            if (existingMarker != null) {
                val meta = existingMarker.marker.metaData()
                        ?: MarkerMetaData(item.item, loadedBitmap = true)
                existingMarker.marker.apply {
                    // manually remove.
                    metaData<T>()?.requestDisposable?.let { toCancel ->
                        imageFetcherDisposeBag.remove(toCancel)
                    }
                    setIcon(item.bitmap)
                    alpha = getMarkerAlpha(floor, displayMode, item.item)
                    tag = meta.copy(item = item.item, loadedBitmap = true)
                }
                return@fromCallable Optional(null)
            } else {
                val holder = constructAndAddMarkerHolder(item = item.item,
                        bitmap = item.bitmap,
                        displayMode = displayMode,
                        map = map,
                        floor = floor,
                        id = id,
                        isSelected = shouldFocusOnCreation(displayMode),
                        loadedBitmap = true,
                        requestDisposable = null)
                synchronized(tempMarkers) {
                    tempMarkers += holder
                }
                return@fromCallable Optional(holder)
            }
        }.delay(100, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
    }

    private fun flushTempMarkers() {
        synchronized(tempMarkers) {
            tempMarkers.forEach { it.marker.remove() }
            tempMarkers.clear()
        }
    }

    private fun removeOldMarkers(existingMapItems: Map<String, MarkerHolder<T>>, items: List<T>): Map<String, MarkerHolder<T>> {
        // separate the list into two separate lists, one for items to be removed
        // one for existing items in the new list too.
        val (existingFoundItems, toBeRemoved) = existingMapItems.values.partition { items.contains(it.item) }

        // trim down items not in the new list
        toBeRemoved.forEach {
            it.marker.tryExpectingFailure(false, Marker::removeWithFadeOut)
        }

        // re-associate the existing found items that are still in the list when a change happens.
        return existingFoundItems.associateBy { it.id }
    }

    /**
     * Constructs the [MarkerHolder] based on a few parameters. Also adds the [MarkerHolder] to
     * the [GoogleMap].
     */
    @UiThread
    internal fun constructAndAddMarkerHolder(
            item: T,
            bitmap: BitmapDescriptor?,
            displayMode: MapDisplayMode,
            map: GoogleMap,
            floor: Int,
            id: String,
            loadedBitmap: Boolean,
            isSelected: Boolean,
            requestDisposable: Disposable?
    ): MarkerHolder<T> {
        val targetAlpha = getMarkerAlpha(floor,
                displayMode,
                item)

        val options = MarkerOptions()
                .zIndex(zIndex)
                .position(getAdjustedLocationFromItem(item))
                .title(item.getContentDescription())
                .icon(bitmap)
                .alpha(0f)

        val markerHolder = MarkerHolder(
                id,
                item,
                map.addMarker(options).apply {
                    tag = MarkerMetaData(item, loadedBitmap, requestDisposable)
                })


        val marker = markerHolder.marker
        marker.fadeIn(targetAlpha)

        /**
         * Focus the selected object.
         */
        if (isSelected) {
            marker.showInfoWindow()
            val currentZoomLevel = map.cameraPosition.zoom
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position,
                    Math.max(ZOOM_INDIVIDUAL, currentZoomLevel)))
        }

        return markerHolder
    }

}