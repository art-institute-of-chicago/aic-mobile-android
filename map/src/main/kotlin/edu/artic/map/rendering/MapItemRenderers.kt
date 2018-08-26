package edu.artic.map.rendering

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.Optional
import com.fuzz.rx.asFlowable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterValue
import com.fuzz.rx.optionalOf
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.VisibleRegion
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.debug
import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapTextType
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.image.asRequestObservable
import edu.artic.image.loadWithThumbnail
import edu.artic.image.toBitmap
import edu.artic.map.ArticObjectMarkerGenerator
import edu.artic.map.DepartmentMarkerGenerator
import edu.artic.map.MapChangeEvent
import edu.artic.map.MapDisplayMode
import edu.artic.map.MapFocus
import edu.artic.map.R
import edu.artic.map.TextMarkerGenerator
import edu.artic.map.amenityIconForAmenityType
import edu.artic.map.getTourOrderNumberBasedOnDisplayMode
import edu.artic.map.helpers.toLatLng
import edu.artic.map.isCloseEnoughToCenter
import edu.artic.ui.util.asCDNUri
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.schedulers.TrampolineScheduler
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val ALPHA_DIMMED = 0.6f
private const val ALPHA_VISIBLE = 1.0f


/**
 * Common logic for non-tour objects.
 * Returns a [Set] of [MapFocus] which results in:
 * If active in search, then always display. We assume when you get to this point, the item should show on the map,
 * and in search mode that is only one item at a time.
 * If on current tour, don't show.
 * @param allContentFocus A lazy evaluated method that only gets run if the we're in [MapDisplayMode.CurrentFloor]
 * @param displayMode The current map's [MapDisplayMode]
 */
internal inline fun searchMapFocus(displayMode: MapDisplayMode, allContentFocus: () -> Set<MapFocus>): Set<MapFocus> =
        when (displayMode) {
            is MapDisplayMode.Tour -> setOf() // don't show
            is MapDisplayMode.Search<*> -> MapFocus.values().toSet() // assume visible if we get here.
            is MapDisplayMode.CurrentFloor -> allContentFocus()
        }

abstract class MapItemRenderer<T>(
        /**
         * Minor optimization to register or not register the bitmap queue within a subclass.
         */
        protected val useBitmapQueue: Boolean = false) {

    protected val mapItems: Subject<Map<String, MarkerHolder<T>>> = BehaviorSubject.createDefault(emptyMap())
    private val currentMap: Subject<Optional<GoogleMap>> = BehaviorSubject.createDefault(Optional(null))
    private var currentFloor: Int = Int.MIN_VALUE

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
                    .buffer(1, TimeUnit.SECONDS, 20)
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
                                            modifiedMarkers[newMarker.id]?.marker?.remove()
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
     * Retrieve location from the item type.
     */
    abstract fun getLocationFromItem(item: T): LatLng

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
    protected fun enqueueBitmapFetch(item: T, mapChangeEvent: MapChangeEvent) {
        getBitmapFetcher(item, mapChangeEvent.displayMode)?.let { fetcher ->
            fetcher
                    .map { DelayedMapItemRenderEvent(mapChangeEvent, item, it) }
                    .subscribeBy { bitmapQueue.onNext(it) }
                    .disposedBy(imageFetcherDisposeBag)
        }
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
                .withLatestFrom(mapItems.toFlowable(BackpressureStrategy.LATEST))
                .map { (mapItemRendererEvent, existingMapItems) ->
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
                        return@map mapAndFetchDisplayMarkers(foundItemsMap, mapItemRendererEvent)
                    }
                }
    }

    /**
     * Maps out any fast [getFastBitmap] markers, will bind those to the [mapItems]. Will enqueue
     * any delayed markers with [enqueueBitmapFetch]. If marker exists on map currently we reuse it
     * and adjust alpha, if needed.
     */
    private fun mapAndFetchDisplayMarkers(foundItemsMap: Map<String, MarkerHolder<T>>,
                                          mapRenderEvent: MapItemRendererEvent<T>): List<MarkerHolder<T>> {
        val (map, mapChangeEvent, items) = mapRenderEvent
        val (_, floor, displayMode) = mapChangeEvent
        return items.mapNotNull { item ->
            val id = getIdFromItem(item)
            val existing = foundItemsMap[id]
            // same item, don't re-add to the map.
            if (existing != null) {
                // adjust alpha, especially for items that are on tour.
                existing.marker.alpha = getMarkerAlpha(floor, displayMode, item)
                existing
            } else {
                enqueueBitmapFetch(item = item, mapChangeEvent = mapChangeEvent)

                // fast bitmap returns immediately.
                getFastBitmap(item, displayMode)?.let { bitmapDescriptor ->
                    constructAndAddMarkerHolder(
                            item = item,
                            bitmap = bitmapDescriptor,
                            displayMode = displayMode,
                            map = map,
                            floor = floor,
                            id = id,
                            // bitmap queue not used, means bitmap is considered loaded
                            loadedBitmap = !useBitmapQueue)
                }
            }
        }
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
                existingMarker.apply {
                    marker.setIcon(item.bitmap)
                    marker.alpha = getMarkerAlpha(floor, displayMode, item.item)
                    marker.tag = MarkerMetaData(item.item, loadedBitmap = true)
                }
                return@fromCallable Optional(null)
            } else {
                val holder = constructAndAddMarkerHolder(item = item.item,
                        bitmap = item.bitmap,
                        displayMode = displayMode,
                        map = map,
                        floor = floor,
                        id = id,
                        loadedBitmap = true)
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
            it.marker.remove()
        }

        // re-associate the existing found items that are still in the list when a change happens.
        val existingFoundItemsMap = existingFoundItems.associateBy { it.id }
        return existingFoundItemsMap
    }

    /**
     * Constructs the [MarkerHolder] based on a few parameters. Also adds the [MarkerHolder] to
     * the [GoogleMap].
     */
    private fun constructAndAddMarkerHolder(
            item: T,
            bitmap: BitmapDescriptor?,
            displayMode: MapDisplayMode,
            map: GoogleMap,
            floor: Int,
            id: String,
            loadedBitmap: Boolean
    ): MarkerHolder<T> {
        val options = MarkerOptions()
                .zIndex(zIndex)
                .position(getLocationFromItem(item))
                .icon(bitmap)
                .alpha(getMarkerAlpha(floor,
                        displayMode,
                        item)
                )
        return MarkerHolder(
                id,
                item,
                map.addMarker(options).apply { tag = MarkerMetaData(item, loadedBitmap) })
    }

}

/**
 * Convenience construct for handling [ArticMapAnnotation] typed objects.
 */
abstract class MapAnnotationItemRenderer(protected val articMapAnnotationDao: ArticMapAnnotationDao,
                                         useBitmapQueue: Boolean = false)
    : MapItemRenderer<ArticMapAnnotation>(useBitmapQueue) {
    override fun getLocationFromItem(item: ArticMapAnnotation): LatLng = item.toLatLng()

    override fun getIdFromItem(item: ArticMapAnnotation): String = item.nid
}

/**
 * Displays Landmark items.
 */
class LandmarkMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val textMarkerGenerator: TextMarkerGenerator by lazy { TextMarkerGenerator(context) }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getTextAnnotationByType(ArticMapTextType.LANDMARK)
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            searchMapFocus(displayMode) { setOf(MapFocus.Landmark) }

    override fun getFastBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty()))

    override val zIndex: Float = ALPHA_VISIBLE
}

class SpacesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao)
    : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val textMarkerGenerator: TextMarkerGenerator  by lazy { TextMarkerGenerator(context) }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getTextAnnotationByTypeAndFloor(ArticMapTextType.SPACE, floor = floor.toString()) // TODO: switch to int
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            searchMapFocus(displayMode) { setOf(MapFocus.DepartmentAndSpaces, MapFocus.Individual) }

    override fun getFastBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty()))

    override val zIndex: Float = ALPHA_VISIBLE
}

class AmenitiesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {
    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getAmenitiesOnMapForFloor(floor = floor.toString())
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> = MapFocus.values().toSet() // all zoom levels

    override fun getFastBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): BitmapDescriptor {
        return BitmapDescriptorFactory.fromResource(amenityIconForAmenityType(item.amenityType))
    }

    override val zIndex: Float = 0.0f
}

class DepartmentsMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao)
    : MapAnnotationItemRenderer(articMapAnnotationDao, useBitmapQueue = true) {

    private val departmentMarkerGenerator: DepartmentMarkerGenerator by lazy { DepartmentMarkerGenerator(context) }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getDepartmentOnMapForFloor(floor = floor.toString())
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            searchMapFocus(displayMode) { setOf(MapFocus.Department, MapFocus.DepartmentAndSpaces) }

    override fun getBitmapFetcher(item: ArticMapAnnotation, displayMode: MapDisplayMode): Observable<BitmapDescriptor>? {
        return Glide.with(context)
                .asBitmap()
                .load(item.imageUrl?.asCDNUri())
                .asRequestObservable(context)
                .map { BitmapDescriptorFactory.fromBitmap(departmentMarkerGenerator.makeIcon(it, item.label.orEmpty())) }
    }

    override val zIndex: Float = 2.0f
}

class GalleriesMapItemRenderer(private val galleriesDao: ArticGalleryDao)
    : MapItemRenderer<ArticGallery>() {

    private val textMarkerGenerator: TextMarkerGenerator by lazy { TextMarkerGenerator(context) }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticGallery>> {
        return galleriesDao.getGalleriesForFloor(floor = floor.toString())
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            searchMapFocus(displayMode) { setOf(MapFocus.Individual) }

    override fun getLocationFromItem(item: ArticGallery): LatLng = item.toLatLng()

    override fun getFastBitmap(item: ArticGallery, displayMode: MapDisplayMode): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.displayTitle))

    override fun getIdFromItem(item: ArticGallery): String = item.galleryId.orEmpty()

    override val zIndex: Float = ALPHA_VISIBLE
}

class TourIntroMapItemRenderer : MapItemRenderer<ArticTour>(useBitmapQueue = true) {

    private val articObjectMarkerGenerator by lazy { ArticObjectMarkerGenerator(context) }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> = when (displayMode) {
        is MapDisplayMode.Tour -> MapFocus.values().toSet()
        else -> setOf()
    }

    override val zIndex: Float = 2.0f

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticTour>> = when (displayMode) {
        is MapDisplayMode.Tour -> listOf(displayMode.tour).asFlowable()
        else -> listOf<ArticTour>().asFlowable()
    }

    override fun getBitmapFetcher(item: ArticTour, displayMode: MapDisplayMode): Observable<BitmapDescriptor>? =
            Glide.with(context)
                    .asBitmap()
                    .load(item.thumbnailFullPath?.asCDNUri())
                    .asRequestObservable(context)
                    .debug("Glide loading: ${item.title}")
                    .map { bitmap ->
                        BitmapDescriptorFactory.fromBitmap(
                                articObjectMarkerGenerator.makeIcon(bitmap))
                    }


    override fun getLocationFromItem(item: ArticTour): LatLng = item.toLatLng()

    override fun getIdFromItem(item: ArticTour): String = item.nid

    override fun getMarkerAlpha(floor: Int, mapDisplayMode: MapDisplayMode, item: ArticTour): Float {
        // on tour, set the alpha depending on current floor.
        return if (mapDisplayMode is MapDisplayMode.Tour) {
            if (item.floorAsInt == floor) ALPHA_VISIBLE else ALPHA_DIMMED
        } else {
            ALPHA_VISIBLE
        }
    }
}

class ObjectsMapItemRenderer(private val objectsDao: ArticObjectDao)
    : MapItemRenderer<ArticObject>(useBitmapQueue = true) {

    private val articObjectMarkerGenerator by lazy { ArticObjectMarkerGenerator(context) }

    private val loadingBitmap by lazy {
        BitmapDescriptorFactory.fromBitmap(
                articObjectMarkerGenerator.makeIcon(null, scale = .7f))
    }
    private val scaledDot by lazy {
        BitmapDescriptorFactory.fromBitmap(
                articObjectMarkerGenerator.makeIcon(null, scale = .15f))
    }

    init {
        // when visible region changes, it's slightly different than mapChanges, because this can
        // happen pretty often.
        visibleRegionChanges
                .sample(500, TimeUnit.MILLISECONDS) // too many events, prevent flooding.
                .withLatestFrom(mapItems, mapChangeEvents)
                .toFlowable(BackpressureStrategy.LATEST) // if downstream can't keep up, let's pick latest.
                .subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (region, mapItems, mapChangeEvent) ->
                    Timber.d("Evaluating Markers For Visible Region Change")
                    mapItems.values.forEach { markerHolder ->
                        val position = getLocationFromItem(markerHolder.item)
                        // set icon to dot.
                        val itemId = getIdFromItem(markerHolder.item)
                        @Suppress("UNCHECKED_CAST")
                        val meta = markerHolder.marker.metaData<ArticObject>()
                        if (position.isCloseEnoughToCenter(region.latLngBounds)) {
                            if (!meta.loadedBitmap) {
                                // show loading while its loading.
                                mapItems[itemId]?.marker?.setIcon(loadingBitmap)

                                // enqueue replacement
                                enqueueBitmapFetch(item = markerHolder.item, mapChangeEvent = mapChangeEvent)
                            }
                        } else {
                            // reset loading state here.
                            mapItems[itemId]?.marker?.apply {
                                tag = meta.copy(loadedBitmap = false)
                                setIcon(scaledDot)
                            }
                        }
                    }
                }
                .disposedBy(imageQueueDisposeBag)
    }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticObject>> = when (displayMode) {
        is MapDisplayMode.CurrentFloor -> objectsDao.getObjectsByFloor(floor = floor)
        is MapDisplayMode.Tour -> objectsDao.getObjectsByIdList(displayMode.tour.tourStops.mapNotNull { it.objectId })
        is MapDisplayMode.Search<*> -> objectsDao.getObjectById((displayMode.item as ArticObject).nid).map { listOf(it) }
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            when (displayMode) {
                is MapDisplayMode.Tour -> MapFocus.values().toSet()
                else -> setOf(MapFocus.Individual)
            }

    override fun getLocationFromItem(item: ArticObject): LatLng = item.toLatLng()

    override fun getIdFromItem(item: ArticObject): String = item.nid

    override fun getFastBitmap(item: ArticObject, displayMode: MapDisplayMode): BitmapDescriptor? =
            loadingBitmap

    override fun getBitmapFetcher(item: ArticObject, displayMode: MapDisplayMode): Observable<BitmapDescriptor>? =
            Glide.with(context)
                    .asBitmap()
                    .apply(RequestOptions().disallowHardwareConfig())
                    .loadWithThumbnail(
                            item.thumbnailFullPath?.asCDNUri(),
                            // Prefer 'image_url', fall back to 'large image' if necessary.
                            (item.image_url ?: item.largeImageFullPath)?.asCDNUri()
                    )
                    .asRequestObservable(context)
                    .map { bitmap ->
                        val order: String? = item.getTourOrderNumberBasedOnDisplayMode(displayMode)
                        BitmapDescriptorFactory.fromBitmap(
                                articObjectMarkerGenerator.makeIcon(bitmap, overlay = order))
                    }

    override val zIndex: Float = 2.0f

    override fun getMarkerAlpha(floor: Int, mapDisplayMode: MapDisplayMode, item: ArticObject): Float {
        // on tour, set the alpha depending on current floor.
        return if (mapDisplayMode is MapDisplayMode.Tour) {
            if (item.floor == floor) ALPHA_VISIBLE else ALPHA_DIMMED
        } else {
            ALPHA_VISIBLE
        }
    }
}

/**
 * Simple [Pair]-like object which holds a [LatLng] and [Int] icon.
 */
data class LionMapItem(val location: LatLng, @DrawableRes val iconId: Int)

class LionMapItemRenderer : MapItemRenderer<LionMapItem>() {

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            MapFocus.values().toSet()

    override val zIndex: Float
        get() = 1.0f

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<LionMapItem>> {
        return Flowable.just(listOf(
                LionMapItem(LatLng(41.879491568164525, -87.624089977901931), R.drawable.map_lion_1),
                LionMapItem(LatLng(41.879678006591391, -87.624091248446064), R.drawable.map_lion_2)))
    }

    override fun getLocationFromItem(item: LionMapItem): LatLng = item.location

    override fun getIdFromItem(item: LionMapItem): String = item.iconId.toString()

    override fun getFastBitmap(item: LionMapItem, displayMode: MapDisplayMode): BitmapDescriptor? {
        return BitmapDescriptorFactory.fromBitmap(
                ContextCompat.getDrawable(context, item.iconId)!!.toBitmap())
    }
}