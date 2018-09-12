package edu.artic.map

import android.content.Context
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.Optional
import com.fuzz.rx.filterValue
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.VisibleRegion
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.map.rendering.AmenitiesMapItemRenderer
import edu.artic.map.rendering.DepartmentsMapItemRenderer
import edu.artic.map.rendering.GalleriesMapItemRenderer
import edu.artic.map.rendering.LandmarkMapItemRenderer
import edu.artic.map.rendering.LionMapItemRenderer
import edu.artic.map.rendering.ObjectsMapItemRenderer
import edu.artic.map.rendering.SpacesMapItemRenderer
import edu.artic.map.rendering.TourIntroMapItemRenderer
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


/**
 * Description: Handles Map population and Marker creation management.
 */
class MapMarkerConstructor
@Inject constructor(articMapAnnotationDao: ArticMapAnnotationDao,
                    galleryDao: ArticGalleryDao,
                    objectsDao: ArticObjectDao) {

    val map: Subject<Optional<GoogleMap>> = BehaviorSubject.createDefault(Optional(null))

    private val disposeBag: DisposeBag = DisposeBag()

    private val landmarkMapItemRenderer = LandmarkMapItemRenderer(articMapAnnotationDao)
    private val spacesMapItemRenderer = SpacesMapItemRenderer(articMapAnnotationDao)
    val amenitiesMapItemRenderer = AmenitiesMapItemRenderer(articMapAnnotationDao)
    private val departmentsMapItemRenderer = DepartmentsMapItemRenderer(articMapAnnotationDao)
    private val galleriesMapItemRenderer = GalleriesMapItemRenderer(galleryDao)
    private val tourIntroMapItemRenderer = TourIntroMapItemRenderer()
    private val lionMapItemRenderer = LionMapItemRenderer()
    val objectsMapItemRenderer = ObjectsMapItemRenderer(objectsDao)

    private val renderers = setOf(landmarkMapItemRenderer, spacesMapItemRenderer,
            amenitiesMapItemRenderer, departmentsMapItemRenderer, galleriesMapItemRenderer,
            objectsMapItemRenderer, tourIntroMapItemRenderer, lionMapItemRenderer)

    /**
     * Bind this when the Fragment view loads.
     */
    fun associateContext(context: Context) {
        renderers.forEach { it.context = context }
    }

    /**
     * Constructs handling for the floor and focus changes on the map.
     */
    fun bindToMapChanges(floorChanges: Observable<Int>,
                         focusChanges: Observable<MapFocus>,
                         displayMode: Observable<MapDisplayMode>,
                         visibleRegion: Observable<VisibleRegion>) {
        // buffer changes between the two events.
        val bufferedMapChangeEvents = Observables.combineLatest(focusChanges, floorChanges, displayMode) { focus, floor, mode ->
            MapChangeEvent(floor = floor, focus = focus, displayMode = mode)
        }
                .toFlowable(BackpressureStrategy.LATEST)
                .share()

        renderers.forEach { renderer ->
            renderer.bindToMapChanges(map.filterValue(), bufferedMapChangeEvents, visibleRegion, disposeBag)
        }
    }

    fun cleanup() {
        renderers.forEach { it.dispose() }
        disposeBag.clear()
    }
}