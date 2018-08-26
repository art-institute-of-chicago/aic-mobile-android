package edu.artic.map

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.Optional
import com.fuzz.rx.asFlowable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.optionalOf
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.debug
import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapTextType
import edu.artic.db.models.ArticObject
import edu.artic.image.asRequestObservable
import edu.artic.image.loadWithThumbnail
import edu.artic.map.helpers.toLatLng
import edu.artic.ui.util.asCDNUri
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class MarkerHolder<T>(val id: String,
                           val item: T,
                           val marker: Marker)

data class MapItemRendererEvent<T>(val map: GoogleMap, val mapChangeEvent: MapChangeEvent, val items: List<T>)
data class DelayedMapItemRenderEvent<T>(val originalEvent: MapItemRendererEvent<T>,
                                        val item: T,
                                        val bitmap: BitmapDescriptor)

abstract class MapItemRenderer<T> {

    private val mapItems: Subject<Map<String, MarkerHolder<T>>> = BehaviorSubject.createDefault(emptyMap())
    private var currentFloor: Int = Int.MIN_VALUE

    private val imageQueueDisposeBag = DisposeBag()
    private val imageFetcherDisposeBag = DisposeBag()
    private val bitmapQueue: Subject<DelayedMapItemRenderEvent<T>> = PublishSubject.create()

    // this should be the inflated view's context
    lateinit var context: Context

    init {
        bitmapQueue
                .buffer(500, TimeUnit.MILLISECONDS, 10)
                .debug("Emitting Bitmap Queue")
                .filter { it.isNotEmpty() }
                .observeOn(AndroidSchedulers.mainThread())
                .withLatestFrom(mapItems)
                .map { (newMarkers, existingMarkers) ->
                    Timber.d("Updating with ${newMarkers.size} to ${existingMarkers.size}")
                    val modifiedMarkers = existingMarkers.toMutableMap()

                    newMarkers
                            .forEach { item ->
                                val id = getIdFromItem(item.item)
                                // only construct things that actually exist in the current mapItems list.
                                // slight chance that the items here will still exist when attemping map
                                // propagation.
                                val existing = existingMarkers[id]
                                if (existing != null) {
                                    // remove existing marker when found, we're overloading the existing one.
                                    existing.marker.remove()
                                }

                                val (map, _, _) = item.originalEvent
                                val (_, floor, displayMode) = item.originalEvent.mapChangeEvent
                                val holder = constructAndAddMarkerHolder(item = item.item,
                                        bitmap = item.bitmap,
                                        displayMode = displayMode,
                                        map = map,
                                        floor = floor,
                                        id = getIdFromItem(item.item))
                                modifiedMarkers[id] = holder
                            }
                    modifiedMarkers
                }
                .bindTo(mapItems)
                .disposedBy(imageQueueDisposeBag)
    }

    /**
     * Return what map focus level these [MapItem] display at.
     */
    abstract fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus>

    abstract val zIndex: Float

    /**
     * Return the specific items that should render based on map floor.
     */
    abstract fun getItemsAtFloor(floor: Int): Flowable<List<T>>


    abstract fun getLocationFromItem(item: T): LatLng

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

    open fun MarkerOptions.configureMarkerOptions(floor: Int, mapDisplayMode: MapDisplayMode, item: T) = Unit

    fun getMarkerHolderById(id: String): Observable<Optional<MarkerHolder<T>>> =
            mapItems.take(1).map { mapItems -> optionalOf(mapItems[id]) }

    fun bindToMapChanges(map: Observable<GoogleMap>, floorFocus: Flowable<MapChangeEvent>, disposeBag: DisposeBag) {
        renderMarkers(map, floorFocus)
                .subscribeBy { updateMarkers(it) }
                .disposedBy(disposeBag)
    }

    fun updateMarkers(markers: List<MarkerHolder<T>>) {
        this.mapItems.onNext(markers.associateBy { it.id })
    }

    @Synchronized
    private fun enqueueBitmapFetch(fetcher: Observable<DelayedMapItemRenderEvent<T>>) {
        fetcher.subscribeBy { bitmapQueue.onNext(it) }
                .disposedBy(imageFetcherDisposeBag)
    }

    private fun renderMarkers(mapObservable: Observable<GoogleMap>, floorFocus: Flowable<MapChangeEvent>)
            : Flowable<List<MarkerHolder<T>>> {
        return floorFocus
                .withLatestFrom(mapObservable.toFlowable(BackpressureStrategy.LATEST)) { first, second -> first to second }
                .debug("New Map Event")
                .observeOn(Schedulers.io())
                .flatMap { (mapEvent, map) ->
                    val visibleMapFocus = getVisibleMapFocus(mapEvent.displayMode)
                    if (!visibleMapFocus.contains(mapEvent.focus)) {
                        Timber.d("Empty list for ${mapEvent.focus} with visible range: $visibleMapFocus")
                        MapItemRendererEvent(map, mapEvent, emptyList<T>()).asFlowable()
                    } else {
                        Timber.d("Getting items at floor ${mapEvent.floor} for ${this::class}")
                        getItemsAtFloor(mapEvent.floor).map {
                            Timber.d("Found ${it.size} items for ${this::class}")
                            MapItemRendererEvent(map, mapEvent, it)
                        }
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .withLatestFrom(mapItems.toFlowable(BackpressureStrategy.LATEST)) { event, mapItems -> event to mapItems }
                .map { (event, existingMapItems) ->
                    val (map, _, items) = event
                    val (_, floor, displayMode) = event.mapChangeEvent
                    // TODO: for now cancel all running image requests.
                    imageFetcherDisposeBag.clear()
                    val foundItemsMap = removeOldMarkers(existingMapItems, items)

                    // don't emit an empty event (which empty zip list does), rather emit an
                    // empty list here.
                    if (items.isEmpty()) {
                        listOf()
                    } else {
                        items.mapNotNull { item ->
                            val id = getIdFromItem(item)
                            val existing = foundItemsMap[id]
                            // same item, don't re-add to the map.
                            if (existing != null) {
                                existing
                            } else {
                                // fetching is enqueued
                                getBitmapFetcher(item, displayMode)?.let { bitmapFetcher ->
                                    enqueueBitmapFetch(bitmapFetcher.map { DelayedMapItemRenderEvent(event, item, it) })
                                }
                                // fast bitmap returns immediately.
                                getFastBitmap(item, displayMode)?.let { bitmap ->
                                    constructAndAddMarkerHolder(
                                            item = item,
                                            bitmap = bitmap,
                                            displayMode = displayMode,
                                            map = map,
                                            floor = floor,
                                            id = id)
                                }
                            }
                        }
                    }
                }
    }

    fun removeOldMarkers(existingMapItems: Map<String, MarkerHolder<T>>, items: List<T>): Map<String, MarkerHolder<T>> {
        // separate the list into two separate lists, one for items to be removed
        // one for existing items in the new list too.
        val (existingFoundItems, toBeRemoved) = existingMapItems.values.partition { items.contains(it.item) }

        // trim down items not in the new list
        toBeRemoved.forEach { it.marker.remove() }

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
            bitmap: BitmapDescriptor,
            displayMode: MapDisplayMode,
            map: GoogleMap,
            floor: Int,
            id: String
    ): MarkerHolder<T> {
        val options = MarkerOptions()
                .zIndex(zIndex)
                .position(getLocationFromItem(item))
                .icon(bitmap)
                .apply {
                    configureMarkerOptions(floor,
                            displayMode,
                            item)
                }
        return MarkerHolder(
                id,
                item,
                map.addMarker(options))
    }

}

/**
 * Convenience construct for handling [ArticMapAnnotation] typed objects.
 */
abstract class MapAnnotationItemRenderer(protected val articMapAnnotationDao: ArticMapAnnotationDao) : MapItemRenderer<ArticMapAnnotation>() {
    override fun getLocationFromItem(item: ArticMapAnnotation): LatLng = item.toLatLng()

    override fun getIdFromItem(item: ArticMapAnnotation): String = item.nid
}

/**
 * Displays Landmark items.
 */
class LandmarkMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val textMarkerGenerator: TextMarkerGenerator by lazy { TextMarkerGenerator(context) }

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getTextAnnotationByType(ArticMapTextType.LANDMARK)
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> = setOf(MapFocus.Landmark)

    override fun getFastBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty()))

    override val zIndex: Float = 1.0f
}

class SpacesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao)
    : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val textMarkerGenerator: TextMarkerGenerator  by lazy { TextMarkerGenerator(context) }

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getTextAnnotationByTypeAndFloor(ArticMapTextType.SPACE, floor = floor.toString()) // TODO: switch to int
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> = setOf(MapFocus.DepartmentAndSpaces, MapFocus.Individual)

    override fun getFastBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty()))

    override val zIndex: Float = 1.0f
}

class AmenitiesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {
    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getAmenitiesOnMapForFloor(floor = floor.toString())
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> = MapFocus.values().toSet() // all zoom levels

    override fun getFastBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): BitmapDescriptor {
        return BitmapDescriptorFactory.fromResource(amenityIconForAmenityType(item.amenityType))
    }

    override val zIndex: Float = 0.0f
}

class DepartmentsMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao)
    : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val departmentMarkerGenerator: DepartmentMarkerGenerator by lazy { DepartmentMarkerGenerator(context) }

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getDepartmentOnMapForFloor(floor = floor.toString())
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> = setOf(MapFocus.Department, MapFocus.DepartmentAndSpaces)

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

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticGallery>> {
        return galleriesDao.getGalleriesForFloor(floor = floor.toString())
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> = setOf(MapFocus.Individual)

    override fun getLocationFromItem(item: ArticGallery): LatLng = item.toLatLng()

    override fun getFastBitmap(item: ArticGallery, displayMode: MapDisplayMode): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.displayTitle))

    override fun getIdFromItem(item: ArticGallery): String = item.galleryId.orEmpty()

    override val zIndex: Float = 1.0f
}

class ObjectsMapItemRenderer(private val objectsDao: ArticObjectDao)
    : MapItemRenderer<ArticObject>() {

    private val articObjectMarkerGenerator by lazy { ArticObjectMarkerGenerator(context) }

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticObject>> {
        return objectsDao.getObjectsByFloor(floor = floor)
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> {
        return if (displayMode is MapDisplayMode.Tour) {
            MapFocus.values().toSet()
        } else {
            setOf(MapFocus.Individual)
        }
    }

    override fun getLocationFromItem(item: ArticObject): LatLng = item.toLatLng()

    override fun getIdFromItem(item: ArticObject): String = item.id.toString()

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
                    .debug("Glide loading: ${item.title}")
                    .map { bitmap ->
                        val order: String? = item.getTourOrderNumberBasedOnDisplayMode(displayMode)
                        BitmapDescriptorFactory.fromBitmap(
                                articObjectMarkerGenerator.makeIcon(bitmap, overlay = order))
                    }

    override val zIndex: Float = 2.0f

    override fun MarkerOptions.configureMarkerOptions(floor: Int, mapDisplayMode: MapDisplayMode, item: ArticObject) {
        // on tour, set the alpha depending on current floor.
        if (mapDisplayMode is MapDisplayMode.Tour) {
            alpha(if (item.floor == floor) 1.0f else 0.6f)
        } else if (mapDisplayMode is MapDisplayMode.CurrentFloor) {
            alpha(1.0f)
        }
    }
}
