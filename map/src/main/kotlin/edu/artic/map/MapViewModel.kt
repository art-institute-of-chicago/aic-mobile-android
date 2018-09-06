package edu.artic.map

import com.fuzz.rx.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.*
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
class MapViewModel @Inject constructor(val mapMarkerConstructor: MapMarkerConstructor,
                                       private val articObjectDao: ArticObjectDao,
                                       val tourProgressManager: TourProgressManager)
    : BaseViewModel() {


    private val floor: Subject<Int> = BehaviorSubject.createDefault(1)
    private val focus: Subject<MapFocus> = BehaviorSubject.create()

    val displayMode: Subject<MapDisplayMode> = PublishSubject.create()
    val selectedArticObject: Subject<ArticObject> = BehaviorSubject.create()
    val individualMapChange: Subject<Optional<Pair<LatLng, MapFocus>>> = PublishSubject.create()
    val tourBoundsChanged: Relay<List<LatLng>> = PublishRelay.create()
    val currentMap: Subject<Optional<GoogleMap>> = BehaviorSubject.createDefault(Optional(null))
    val leaveTourRequest: Subject<Boolean> = PublishSubject.create()
    val switchTourRequest: Subject<Pair<ArticTour,ArticTour>> = PublishSubject.create()

    val distinctFloor: Observable<Int>
        get() = floor.distinctUntilChanged()

    val selectedTourStopMarkerId: Subject<String> = BehaviorSubject.create()

    private val visibleRegionChanges: Subject<VisibleRegion> = BehaviorSubject.create()

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

        displayMode
                .distinctUntilChanged()
                .filterFlatMap({ it is MapDisplayMode.Tour }, { it as MapDisplayMode.Tour })
                .filterFlatMap({ it.selectedTourStop?.objectId != null }, { it.selectedTourStop?.objectId })
                .map { it }
                .bindTo(tourProgressManager.selectedStop)
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

        tourProgressManager
                .leaveTourRequest
                .bindTo(leaveTourRequest)
                .disposedBy(disposeBag)


        this.currentMap
                .bindTo(mapMarkerConstructor.map)
                .disposedBy(disposeBag)
    }

    fun setMap(map: GoogleMap) {
        currentMap.onNext(optionalOf(map))
    }

    fun floorChangedTo(floor: Int) {
        this.floor.onNext(floor)
    }

    fun zoomLevelChanged(zoomLevel: ZoomLevel) {
        this.focus.onNext(zoomLevel.toMapFocus())
    }

    fun departmentMarkerSelected(department: ArticMapAnnotation) {
        individualMapChange.onNext(optionalOf(department.toLatLng() to MapFocus.Individual))
    }

    fun articObjectSelected(articObject: ArticObject) {
        lockVisibleRegion.onNext(true)
        selectedArticObject.onNext(articObject)
    }

    fun displayModeChanged(displayMode: MapDisplayMode) {
        this.displayMode.onNext(displayMode)
        if (displayMode is MapDisplayMode.Tour) {
            animateToTourStopBounds(displayMode)
        }
    }

    private fun animateToTourStopBounds(displayMode: MapDisplayMode.Tour) {
        val tour = displayMode.tour
        val tourStop = displayMode.selectedTourStop ?: tour.getIntroStop()

        val latLongs: Observable<List<LatLng>> = if (tourStop.isIntroStop()) {
            /** If the starting tour is intro stop load map in bird eye view**/
            val allToursLatLongs = articObjectDao
                    .getObjectsByIdList(tour.tourStops.mapNotNull { it.objectId })
                    .map { stops -> stops.map { it.toLatLng() } }
                    .toObservable()

            Observable.merge(
                    Observable.just(listOf(tour.toLatLng())),
                    allToursLatLongs
            )
        } else {
            articObjectDao.getObjectById(tourStop.objectId!!)
                    .toObservable()
                    .map { listOf(it.toLatLng()) }
        }

        latLongs
                .bindToMain(tourBoundsChanged)
                .disposedBy(disposeBag)
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

    fun leaveTour() {
        tourProgressManager.selectedTour.onNext(Optional(null))
        displayModeChanged(MapDisplayMode.CurrentFloor)
    }

    /**
     * Updates the display mode of the map, based on the requestedTour.
     * For future search integration, active tour should be canceled before we display the search items in map.
     * refer [TourProgressManager] for managing the tour state.
     */
    fun loadMapDisplayMode(requestedTour: ArticTour?, tourStop: ArticTour.TourStop?) {
        tourProgressManager.selectedTour
                .take(1)
                .subscribeBy { lastTour ->
                    /**
                     * If requestedTour is different than current tour display prompt user to leave the previous requestedTour.
                     */
                    val activeTour = lastTour.value
                    if (requestedTour != null && activeTour != null && requestedTour != activeTour) {
                        switchTourRequest.onNext(activeTour to requestedTour)
                    } else {
                        val tourToLoad = requestedTour ?: activeTour
                        if (tourToLoad != null) {
                            displayModeChanged(MapDisplayMode.Tour(tourToLoad, tourStop))
                        } else {
                            displayModeChanged(MapDisplayMode.CurrentFloor)
                        }
                    }

                }.disposedBy(disposeBag)

    }
}