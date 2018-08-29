package edu.artic.map

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.Optional
import com.fuzz.rx.asFlowable
import com.fuzz.rx.asObservable
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
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber

data class MarkerHolder<T>(val id: String,
                           val item: T,
                           val marker: Marker)

data class MapItemRendererEvent<T>(val map: GoogleMap, val mapChangeEvent: MapChangeEvent, val items: List<T>)

abstract class MapItemRenderer<T> {

    private val mapItems: Subject<Map<String, MarkerHolder<T>>> = BehaviorSubject.createDefault(emptyMap())
    private var currentFloor: Int = Int.MIN_VALUE
    private val disposeBag: DisposeBag = DisposeBag()

    // this should be the inflated view's context
    lateinit var context: Context

    /**
     * Return what map focus level these [MapItem] display at.
     */
    abstract val visibleMapFocus: Set<MapFocus>

    abstract val zIndex: Float

    /**
     * Return the specific items that should render based on map floor.
     */
    abstract fun getItemsAtFloor(floor: Int): Flowable<List<T>>


    abstract fun getLocationFromItem(item: T): LatLng

    abstract fun getIdFromItem(item: T): String

    abstract fun getBitmap(item: T, displayMode: MapDisplayMode): Observable<BitmapDescriptor>

    open fun MarkerOptions.configureMarkerOptions(floor: Int, mapDisplayMode: MapDisplayMode, item: T) = Unit

    fun getMarkerHolderById(id: String): Observable<Optional<MarkerHolder<T>>> =
            mapItems.take(1).map { mapItems -> optionalOf(mapItems[id]) }

    fun updateMarkers(markers: List<MarkerHolder<T>>) {
        this.mapItems.onNext(markers.associateBy { it.id })
    }

    fun renderMarkers(map: Observable<GoogleMap>, floorFocus: Flowable<MapChangeEvent>)
            : Observable<List<MarkerHolder<T>>> {
        return floorFocus
                .withLatestFrom(map.toFlowable(BackpressureStrategy.LATEST)) { first, second -> first to second }
                .debug("New Map Event")
                .observeOn(Schedulers.io())
                .flatMap { (mapEvent, map) ->
                    // no longer renderable, we
                    if (!visibleMapFocus.contains(mapEvent.focus)) {
                        Timber.d("Empty list for ${mapEvent.focus} with visible range: $visibleMapFocus")
                        MapItemRendererEvent(map, mapEvent, emptyList<T>()).asFlowable()
                    } else {
                        Timber.d("Getting items at floor ${mapEvent.floor}")
                        getItemsAtFloor(mapEvent.floor).map { MapItemRendererEvent(map, mapEvent, it) }
                    }
                }
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .debug("Received Items")
                .withLatestFrom(mapItems) { event, mapItems -> event to mapItems }
                .debug("Getting Existing Items")
                .doOnNext { (event, existingMapItems) ->

                    // returns items not in the new list.
                    val list = (existingMapItems.values - event.items)
                    @Suppress("UNCHECKED_CAST")
                    (list as List<MarkerHolder<T>>)
                            .forEach { it.marker.remove() }
                }
                .flatMap { (event, existingMapItems) ->
                    Observable.zip(event.items.map { item ->
                        val id = getIdFromItem(item)
                        val existing = existingMapItems[id]
                        // same item, don't re-add to the map.
                        existing?.asObservable()
                                ?: getBitmap(item, event.mapChangeEvent.displayMode)
                                        .map { bitmap ->
                                            val options = MarkerOptions()
                                                    .zIndex(zIndex)
                                                    .position(getLocationFromItem(item))
                                                    .icon(bitmap)
                                                    .apply {
                                                        configureMarkerOptions(event.mapChangeEvent.floor,
                                                                event.mapChangeEvent.displayMode,
                                                                item)
                                                    }
                                            MarkerHolder(
                                                    id,
                                                    item,
                                                    event.map.addMarker(options))
                                        }

                    }) { markers ->
                        // RX Zip does not return a properly typed array, it is an Object[] from this zip.
                        @Suppress("UNCHECKED_CAST")
                        (markers as Array<Any>)
                                .mapTo(mutableListOf()) { item -> item as MarkerHolder<T> }
                                .toList()
                    }
                }
                .debug("Constructed Map MarkerHolders")
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
        return articMapAnnotationDao.getAnnotationByTypeForFloor(ArticMapTextType.LANDMARK, floor = floor.toString()) // TODO: switch to int
    }

    override val visibleMapFocus: Set<MapFocus> = setOf(MapFocus.Landmark)

    override fun getBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): Observable<BitmapDescriptor> =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty())).asObservable()

    override val zIndex: Float = 1.0f
}

class SpacesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao)
    : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val textMarkerGenerator: TextMarkerGenerator  by lazy { TextMarkerGenerator(context) }

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getAnnotationByTypeForFloor(ArticMapTextType.SPACE, floor = floor.toString())
    }

    override val visibleMapFocus: Set<MapFocus>
        get() = setOf(MapFocus.DepartmentAndSpaces, MapFocus.Individual)

    override fun getBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): Observable<BitmapDescriptor> =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty())).asObservable()

    override val zIndex: Float = 1.0f
}

class AmenitiesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {
    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getAmenitiesOnMapForFloor(floor = floor.toString())
    }

    override val visibleMapFocus: Set<MapFocus> = MapFocus.values().toSet() // all zoom levels

    override fun getBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): Observable<BitmapDescriptor> {
        return BitmapDescriptorFactory.fromResource(amenityIconForAmenityType(item.amenityType)).asObservable()
    }

    override val zIndex: Float = 0.0f
}

class DepartmentsMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao)
    : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val departmentMarkerGenerator: DepartmentMarkerGenerator by lazy { DepartmentMarkerGenerator(context) }

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getDepartmentOnMapForFloor(floor = floor.toString())
    }

    override val visibleMapFocus: Set<MapFocus> = setOf(MapFocus.Department, MapFocus.DepartmentAndSpaces)

    override fun getBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): Observable<BitmapDescriptor> {
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

    override val visibleMapFocus: Set<MapFocus> = setOf(MapFocus.Individual)

    override fun getLocationFromItem(item: ArticGallery): LatLng = item.toLatLng()

    override fun getBitmap(item: ArticGallery, displayMode: MapDisplayMode): Observable<BitmapDescriptor> =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.number.orEmpty())).asObservable()

    override fun getIdFromItem(item: ArticGallery): String = item.galleryId.orEmpty()

    override val zIndex: Float = 1.0f
}

class ObjectsMapItemRenderer(private val objectsDao: ArticObjectDao)
    : MapItemRenderer<ArticObject>() {

    private val articObjectMarkerGenerator by lazy { ArticObjectMarkerGenerator(context) }

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticObject>> {
        return objectsDao.getObjectsByFloor(floor = floor)
    }

    override val visibleMapFocus: Set<MapFocus>
        get() = setOf(MapFocus.Individual)

    override fun getLocationFromItem(item: ArticObject): LatLng = item.toLatLng()

    override fun getIdFromItem(item: ArticObject): String = item.id.toString()

    override fun getBitmap(item: ArticObject, displayMode: MapDisplayMode): Observable<BitmapDescriptor> =
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
                        var order: String? = null
                        if (displayMode is MapDisplayMode.Tour) {
                            /**
                             * If map's display mode is Tour, get the order number of the stop.
                             */
                            val index = displayMode.tour
                                    .tourStops
                                    .indexOfFirst { it.objectId == item.nid }

                            if (index > -1) {
                                order = (index + 1).toString()
                            }
                        }
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
