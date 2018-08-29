package edu.artic.map

import com.fuzz.rx.DisposeBag
import com.fuzz.rx.asFlowable
import com.fuzz.rx.bindTo
import com.google.android.gms.maps.GoogleMap
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticMapAnnotation
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import javax.inject.Inject


/**
 * Description: Handles Map population and Marker creation management.
 */
class ExploreMapMarkerConstructor
@Inject constructor(private val articMapAnnotationDao: ArticMapAnnotationDao,
                    private val galleryDao: ArticGalleryDao,
                    private val objectsDao: ArticObjectDao) {


    lateinit var map: GoogleMap

    private val disposeBag: DisposeBag = DisposeBag()

    private val landmarkMapItemRenderer = LandmarkMapItemRenderer(articMapAnnotationDao)
    private val spacesMapItemRenderer = SpacesMapItemRenderer(articMapAnnotationDao)
    private val amenitiesMapItemRenderer = AmenitiesMapItemRenderer(articMapAnnotationDao)
    private val departmentsMapItemRenderer = DepartmentsMapItemRenderer(articMapAnnotationDao)
    private val galleriesMapItemRenderer = GalleriesMapItemRenderer(galleryDao)
    private val objectsMapItemRenderer = ObjectsMapItemRenderer(objectsDao)


    /**
     * Constructs handling for the floor and focus changes on the map.
     */
    fun bindToMapChanges(floorChanges: Observable<Int>,
                         focusChanges: Observable<MapFocus>) {
        // buffer changes between the two events.
        val bufferedFloorFocus = Observables.combineLatest(focusChanges, floorChanges)
                .toFlowable(BackpressureStrategy.LATEST)

        bufferedFloorFocus
                .flatMap { (focus, floor) ->
                    // no longer renderable, we
                    if (!landmarkMapItemRenderer.visibleMapFocus.contains(focus)) {
                        emptyList<ArticMapAnnotation>().asFlowable()
                    } else {
                        landmarkMapItemRenderer.getAnnotationsAtFloor(floor)
                    }
                }
                .map { items ->
                    items.map {

                    }
                }
                .bindTo()
                .disposedBy(disposeBag)

    }
}