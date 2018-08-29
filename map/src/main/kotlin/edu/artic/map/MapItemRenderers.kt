package edu.artic.map

import com.google.android.gms.maps.model.Marker
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapTextType
import edu.artic.map.helpers.mapToMapItem
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

abstract class MapItemRenderer {

    private val mapItems: Subject<List<Marker>> = BehaviorSubject.createDefault(emptyList())
    private var currentFloor: Int = Int.MIN_VALUE

    /**
     * Return the specific items that should render based on map floor.
     */
    abstract fun getItemsAtFloor(floor: Int): Flowable<List<MapItem<*>>>

    /**
     * Return what map focus level these [MapItem] display at.
     */
    abstract val visibleMapFocus: Set<MapFocus>

}

/**
 * Convenience construct for handling [ArticMapAnnotation] typed objects.
 */
abstract class MapAnnotationItemRenderer(protected val articMapAnnotationDao: ArticMapAnnotationDao) : MapItemRenderer() {

    override fun getItemsAtFloor(floor: Int): Flowable<List<MapItem<*>>> {
        return getAnnotationsAtFloor(floor).map { annotations -> annotations.mapToMapItem() }
    }

    abstract fun getAnnotationsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>>
}

/**
 * Displays Landmark items.
 */
class LandmarkMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {

    override fun getAnnotationsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getAnnotationByTypeForFloor(ArticMapTextType.LANDMARK, floor = floor.toString()) // TODO: switch to int
    }

    override val visibleMapFocus: Set<MapFocus> = setOf(MapFocus.Landmark)
}

class SpacesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {
    override fun getAnnotationsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getAnnotationByTypeForFloor(ArticMapTextType.SPACE, floor = floor.toString())
    }

    override val visibleMapFocus: Set<MapFocus>
        get() = setOf(MapFocus.DepartmentAndSpaces, MapFocus.Individual)
}

class AmenitiesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {
    override fun getAnnotationsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getAmenitiesOnMapForFloor(floor = floor.toString())
    }

    override val visibleMapFocus: Set<MapFocus> = MapFocus.values().toSet() // all zoom levels
}

class DepartmentsMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {
    override fun getAnnotationsAtFloor(floor: Int): Flowable<List<ArticMapAnnotation>> {
        return articMapAnnotationDao.getDepartmentOnMapForFloor(floor = floor.toString())
    }

    override val visibleMapFocus: Set<MapFocus> = setOf(MapFocus.Department, MapFocus.DepartmentAndSpaces)
}

class GalleriesMapItemRenderer(private val galleriesDao: ArticGalleryDao) : MapItemRenderer() {
    override fun getItemsAtFloor(floor: Int): Flowable<List<MapItem<*>>> {
        return galleriesDao.getGalleriesForFloor(floor = floor.toString())
                .map { galleries -> galleries.map { MapItem.Gallery(it, floor) } }
    }

    override val visibleMapFocus: Set<MapFocus> = setOf(MapFocus.Individual)
}

class ObjectsMapItemRenderer(private val objectsDao: ArticObjectDao) : MapItemRenderer() {
    override fun getItemsAtFloor(floor: Int): Flowable<List<MapItem<*>>> {
        return objectsDao.getObjectsByFloor(floor = floor)
                .map { objects -> objects.map { MapItem.Object(it, floor) } }
    }

    override val visibleMapFocus: Set<MapFocus>
        get() = setOf(MapFocus.Individual)
}