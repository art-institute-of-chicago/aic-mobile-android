package edu.artic.map

import android.content.Context
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.Optional
import com.fuzz.rx.debug
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterValue
import com.google.android.gms.maps.GoogleMap
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


/**
 * Description: Handles Map population and Marker creation management.
 */
class ExploreMapMarkerConstructor
@Inject constructor(private val articMapAnnotationDao: ArticMapAnnotationDao,
                    private val galleryDao: ArticGalleryDao,
                    private val objectsDao: ArticObjectDao,
                    private val appContext: Context) { // App context, won't leak.


    val map: Subject<Optional<GoogleMap>> = BehaviorSubject.createDefault(Optional(null))

    private val disposeBag: DisposeBag = DisposeBag()

    private val landmarkMapItemRenderer = LandmarkMapItemRenderer(articMapAnnotationDao, appContext)
    private val spacesMapItemRenderer = SpacesMapItemRenderer(articMapAnnotationDao, appContext)
    private val amenitiesMapItemRenderer = AmenitiesMapItemRenderer(articMapAnnotationDao)
    private val departmentsMapItemRenderer = DepartmentsMapItemRenderer(articMapAnnotationDao, appContext)
    private val galleriesMapItemRenderer = GalleriesMapItemRenderer(galleryDao, appContext)
    val objectsMapItemRenderer = ObjectsMapItemRenderer(objectsDao, appContext)

    /**
     * Constructs handling for the floor and focus changes on the map.
     */
    fun bindToMapChanges(floorChanges: Observable<Int>,
                         focusChanges: Observable<MapFocus>,
                         displayMode: Observable<MapDisplayMode>) {
        // buffer changes between the two events.
        val bufferedFloorFocus = Observables.combineLatest(focusChanges, floorChanges, displayMode) { focus, floor, mode ->
            MapChangeEvent(floor = floor, focus = focus, displayMode = mode)
        }
                .toFlowable(BackpressureStrategy.LATEST)
                .share()

        landmarkMapItemRenderer.renderMarkers(map.debug("Map Changed").filterValue(), bufferedFloorFocus)
                .subscribeBy { landmarkMapItemRenderer.updateMarkers(it) }
                .disposedBy(disposeBag)

        spacesMapItemRenderer.renderMarkers(map.debug("Map Changed").filterValue(), bufferedFloorFocus)
                .subscribeBy { spacesMapItemRenderer.updateMarkers(it) }
                .disposedBy(disposeBag)
    }
}