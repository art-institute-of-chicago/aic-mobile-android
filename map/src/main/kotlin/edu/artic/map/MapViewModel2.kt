package edu.artic.map

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.fuzz.rx.mapOptional
import com.fuzz.rx.optionalOf
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticObject
import edu.artic.map.carousel.TourProgressManager
import edu.artic.map.helpers.toLatLng
import edu.artic.map.rendering.MarkerHolder
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * Description:
 */
class MapViewModel2 @Inject constructor(val mapMarkerConstructor: MapMarkerConstructor,
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

    private val visibleRegionChanges: Subject<VisibleRegion> = PublishSubject.create()

    // when set, normal visible region changes are locked until, say the map move completes.
    private val lockVisibleRegion: Subject<Boolean> = BehaviorSubject.createDefault(false)

    init {
        mapMarkerConstructor.bindToMapChanges(distinctFloor,
                focus.distinctUntilChanged(),
                displayMode.distinctUntilChanged(),
                visibleRegionChanges.distinctUntilChanged())

        // when we change to tour mode, we notify the tourProgressManager.
        displayMode
                .distinctUntilChanged()
                .filterFlatMap({ it is MapDisplayMode.Tour }, { (it as MapDisplayMode.Tour).tour })
                .doOnNext { floorChangedTo(it.floorAsInt) }
                .mapOptional()
                .bindTo(tourProgressManager.selectedTour)
                .disposedBy(disposeBag)

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

    fun setMap(map: GoogleMap) {
        mapMarkerConstructor.map.onNext(optionalOf(map))
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
        lockVisibleRegion.onNext(true)
        selectedArticObject.onNext(articObject)
    }

    fun visibleRegionChanged(visibleRegion: VisibleRegion) {
        // only emit if visible region is not locked.
        lockVisibleRegion
                .filter { !it }
                .subscribeBy {
                    visibleRegionChanges.onNext(visibleRegion)
                }
                .disposedBy(disposeBag)
    }

    fun visibleRegionIdle(visibleRegion: VisibleRegion) {
        this.lockVisibleRegion.onNext(false)
        visibleRegionChanged(visibleRegion)
    }

    /**
     * Retrieve an object from the [MapMarkerConstructor]
     */
    fun retrieveObjectById(nid: String): Observable<Optional<MarkerHolder<ArticObject>>> {
        return mapMarkerConstructor.objectsMapItemRenderer.getMarkerHolderById(nid)
    }

    override fun onCleared() {
        super.onCleared()
        mapMarkerConstructor.cleanup()
    }


}