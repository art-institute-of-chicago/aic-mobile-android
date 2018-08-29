package edu.artic.map

import android.content.Context
import com.bumptech.glide.Glide
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
import edu.artic.map.helpers.toLatLng
import edu.artic.ui.util.asCDNUri
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

data class MarkerHolder<T>(val item: T, val marker: Marker)

abstract class MapItemRenderer<T> {

    private val mapItems: Subject<List<Marker>> = BehaviorSubject.createDefault(emptyList())
    private var currentFloor: Int = Int.MIN_VALUE
    private val disposeBag: DisposeBag = DisposeBag()

    /**
     * Return the specific items that should render based on map floor.
     */
    abstract fun getItemsAtFloor(floor: Int): Flowable<List<T>>

    /**
     * Return what map focus level these [MapItem] display at.
     */
    abstract val visibleMapFocus: Set<MapFocus>

    abstract fun getLocationFromItem(item: T): LatLng

    abstract fun getBitmap(item: T): Observable<BitmapDescriptor>

    fun renderMarkers(map: GoogleMap, floorFocus: Flowable<Pair<MapFocus, Int>>)
            : Observable<List<MarkerHolder<T>>> {
        return floorFocus
                .flatMap { (focus, floor) ->
                    // no longer renderable, we
                    if (!visibleMapFocus.contains(focus)) {
                        emptyList<T>().asFlowable()
                    } else {
                        getItemsAtFloor(floor)
                    }
                }
                .toObservable()
                .flatMap { items ->
                    Observable.zip(items.map { item ->
                        getBitmap(item).map { bitmap ->
                            MarkerHolder(marker = map.addMarker(MarkerOptions()
                                    .position(getLocationFromItem(item))
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

    override fun getBitmap(item: ArticMapAnnotation): Observable<BitmapDescriptor> =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty())).asObservable()
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

    override fun getBitmap(item: ArticMapAnnotation): Observable<BitmapDescriptor> =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty())).asObservable()
}

class AmenitiesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {
    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getAmenitiesOnMapForFloor(floor = floor.toString())
    }

    override val visibleMapFocus: Set<MapFocus> = MapFocus.values().toSet() // all zoom levels

    override fun getBitmap(item: ArticMapAnnotation): Observable<BitmapDescriptor> {
        return BitmapDescriptorFactory.fromResource(amenityIconForAmenityType(item.amenityType)).asObservable()
    }
}

class DepartmentsMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao,
                                 private val context: Context)
    : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val departmentMarkerGenerator: DepartmentMarkerGenerator = DepartmentMarkerGenerator(context)

    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getDepartmentOnMapForFloor(floor = floor.toString())
    }

    override val visibleMapFocus: Set<MapFocus> = setOf(MapFocus.Department, MapFocus.DepartmentAndSpaces)

    override fun getBitmap(item: ArticMapAnnotation): Observable<BitmapDescriptor> {
        return Glide.with(context)
                .asBitmap()
                .load(item.imageUrl?.asCDNUri())
                .asRequestObservable(context)
                .map { BitmapDescriptorFactory.fromBitmap(it) }
    }
}

class GalleriesMapItemRenderer(private val galleriesDao: ArticGalleryDao,
                               private val context: Context)
    : MapItemRenderer<ArticGallery>() {


    override fun getItemsAtFloor(floor: Int): Flowable<List<ArticGallery>> {
        return galleriesDao.getGalleriesForFloor(floor = floor.toString())
    }

    override val visibleMapFocus: Set<MapFocus> = setOf(MapFocus.Individual)
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
}