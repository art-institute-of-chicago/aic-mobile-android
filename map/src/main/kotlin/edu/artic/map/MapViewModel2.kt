package edu.artic.map

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.optionalOf
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticObject
import edu.artic.map.carousel.TourProgressManager
import edu.artic.map.helpers.toLatLng
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * Description:
 */
class MapViewModel2 @Inject constructor(private val mapMarkerConstructor: ExploreMapMarkerConstructor,
                                        private val tourProgressManager: TourProgressManager)
    : BaseViewModel() {


    private val floor: Subject<Int> = BehaviorSubject.createDefault(1)
    private val focus: Subject<MapFocus> = BehaviorSubject.create()

    val displayMode: Subject<MapDisplayMode> = BehaviorSubject.create()
    val selectedArticObject: Subject<ArticObject> = BehaviorSubject.create()
    val cameraMovementRequested: Subject<Optional<Pair<LatLng, MapFocus>>> = PublishSubject.create()
    val distinctFloor: Observable<Int>
        get() = floor.distinctUntilChanged()

    val selectedTourStopMarkerId: Subject<String> = BehaviorSubject.create()

    init {
        mapMarkerConstructor.bindToMapChanges(floor, focus, displayMode)

        /**
         * Sync the selected tour stop with the carousel.
         */
        selectedArticObject
                .distinctUntilChanged()
                .map { it.nid }
                .bindTo(tourProgressManager.selectedStop)
                .disposedBy(disposeBag)

        /**
         * Sync the carousel tour stop with map tour stop.
         */
        tourProgressManager.selectedStop
                .distinctUntilChanged()
                .bindTo(selectedTourStopMarkerId)
                .disposedBy(disposeBag)
    }

    fun floorChangedTo(floor: Int) {
        this.floor.onNext(floor)
    }

    fun zoomLevelChanged(zoomLevel: ZoomLevel) {
        this.focus.onNext(zoomLevel.toMapFocus())
    }

    fun departmentMarkerSelected(department: ArticMapAnnotation) {
        cameraMovementRequested.onNext(optionalOf(department.toLatLng() to MapFocus.Department))
    }

    fun articObjectSelected(articObject: ArticObject) {
        selectedArticObject.onNext(articObject)
    }
}