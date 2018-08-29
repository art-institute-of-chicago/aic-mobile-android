package edu.artic.map

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.asFlowable
import com.fuzz.rx.asObservable
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapTextType
import edu.artic.db.models.ArticObject
import edu.artic.image.asRequestObservable
import edu.artic.image.loadWithThumbnail
import edu.artic.map.helpers.toLatLng
import edu.artic.ui.util.asCDNUri
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

data class MarkerHolder<T>(val id: String,
                           val item: T,
                           val marker: Marker)

abstract class MapItemRenderer<T> {

    private val mapItems: Subject<Map<String, MarkerHolder<T>>> = BehaviorSubject.createDefault(emptyMap())
    private var currentFloor: Int = Int.MIN_VALUE
    private val disposeBag: DisposeBag = DisposeBag()

    protected val alpha: Float = 1.0f

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

    fun updateMarkers(markers: List<MarkerHolder<T>>) {
        this.mapItems.onNext(markers.associateBy { it.id })
    }

    fun renderMarkers(map: GoogleMap, floorFocus: Flowable<MapChangeEvent>)
            : Observable<List<MarkerHolder<T>>> {
        return floorFocus
                .observeOn(Schedulers.io())
                .flatMap { (focus, floor, displayMode) ->
                    // no longer renderable, we
                    if (!visibleMapFocus.contains(focus)) {
                        (emptyList<T>() to displayMode).asFlowable()
                    } else {
                        getItemsAtFloor(floor).zipWith(displayMode.asFlowable())
                    }
                }
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .withLatestFrom(mapItems) { (newItems, displayMode), mapItems -> Triple(newItems, displayMode, mapItems) }
                .doOnNext { (newItems, _, existingMapItems) ->

                    // returns items not in the new list.
                    @Suppress("UNCHECKED_CAST")
                    ((existingMapItems.toList() - newItems) as List<MarkerHolder<T>>)
                            .forEach { it.marker.remove() }
                }
                .flatMap { (newItems, displayMode, existingMapItems) ->
                    Observable.zip(newItems.map { item ->
                        val id = getIdFromItem(item)
                        val existing = existingMapItems[id]
                        // same item, don't re-add to the map.
                        existing?.asObservable()
                                ?: getBitmap(item, displayMode).map { bitmap ->
                                    MarkerHolder(
                                            id = id,
                                            marker = map.addMarker(
                                                    MarkerOptions()
                                                            .zIndex(zIndex)
                                                            .position(getLocationFromItem(item))
                                                            .alpha(alpha)
                                                            .icon(bitmap)),
                                            item = item)
                                }

                    }) { markers ->
                        @Suppress("UNCHECKED_CAST")
                        markers as List<MarkerHolder<T>>
                    }
                }
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
class LandmarkMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao, context: Context) : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val textMarkerGenerator: TextMarkerGenerator = TextMarkerGenerator(context)

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getAnnotationByTypeForFloor(ArticMapTextType.LANDMARK, floor = floor.toString()) // TODO: switch to int
    }

    override val visibleMapFocus: Set<MapFocus> = setOf(MapFocus.Landmark)

    override fun getBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): Observable<BitmapDescriptor> =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty())).asObservable()

    override val zIndex: Float = 1.0f
}

class SpacesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao,
                            private val context: Context)
    : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val textMarkerGenerator: TextMarkerGenerator = TextMarkerGenerator(context)

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

class DepartmentsMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao,
                                 private val context: Context)
    : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val departmentMarkerGenerator: DepartmentMarkerGenerator = DepartmentMarkerGenerator(context)

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

class GalleriesMapItemRenderer(private val galleriesDao: ArticGalleryDao,
                               private val context: Context)
    : MapItemRenderer<ArticGallery>() {

    private val textMarkerGenerator: TextMarkerGenerator = TextMarkerGenerator(context)
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

class ObjectsMapItemRenderer(private val objectsDao: ArticObjectDao,
                             private val context: Context)
    : MapItemRenderer<ArticObject>() {

    private val articObjectMarkerGenerator = ArticObjectMarkerGenerator(context)

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
}

