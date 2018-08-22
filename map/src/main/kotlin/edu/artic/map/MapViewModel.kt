package edu.artic.map

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.google.android.gms.maps.model.LatLng
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.models.*
import edu.artic.map.carousel.TourProgressManager
import edu.artic.map.helpers.mapToMapItem
import edu.artic.map.helpers.toLatLng
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * Backing logic for the markers we display in [MapFragment].
 *
 * What we display there is informed directly by the [zoomLevel] and
 * the [floor]. Observables defined in this class will (as a rule)
 * only emit an event if those properties change.
 *
 * Everything starts with `init`.
 *
 * @see MapItem
 */
class MapViewModel @Inject constructor(
        private val mapAnnotationDao: ArticMapAnnotationDao,
        private val galleryDao: ArticGalleryDao,
        private val objectDao: ArticObjectDao,
        tourDao: ArticTourDao,
        tourProgressManager: TourProgressManager
) : BaseViewModel() {

    val amenities: Subject<List<MapItem.Annotation>> = BehaviorSubject.create()
    val spacesAndLandmarks: Subject<List<MapItem.Annotation>> = BehaviorSubject.create()
    val selectedArticObject: Subject<ArticObject> = BehaviorSubject.create()

    val displayMode: Subject<DisplayMode> = BehaviorSubject.create()
    val centerFullObjectMarker: Subject<String> = BehaviorSubject.create()

    /**
     * Class used to represent the display mode of map.
     * Map view can be used with following different modes
     *
     * <ul>
     *     <li>General: Everything is displayed for selected floor </li>
     *     <li>Tour   : Only the tour tourStopViewModels are displayed in the map.</li>
     * </ul>
     */
    sealed class DisplayMode {
        class General() : DisplayMode()
        class Tour(val tour: ArticTour) : DisplayMode()
    }

    /**
     * This stores all the map items, and changes at every zoom level and every floor.
     *
     * We strongly recommend familiarity with the [MapItem] type before using this field. This
     * Observable is expected to emit events rather frequently, so for the best performance
     * you'll probably want to minimize allocations in whatever you have observing it.
     */
    val whatToDisplayOnMap: Subject<List<MapItem<*>>> = BehaviorSubject.create()


    private val floor: Subject<Int> = BehaviorSubject.createDefault(1)
    val distinctFloor: Observable<Int>
        get() = floor.distinctUntilChanged()

    val zoomLevel: Subject<MapZoomLevel> = BehaviorSubject.create()

    val cameraMovementRequested: Subject<Optional<Pair<LatLng, MapZoomLevel>>> = BehaviorSubject.create()

    val currentFloor: Int
        get() {
            val castFloor = (floor as BehaviorSubject)
            return if (castFloor.hasValue()) {
                castFloor.value
            } else {
                1
            }
        }

    val currentZoomLevel: MapZoomLevel
        get() {
            val castLevel = (zoomLevel as BehaviorSubject)
            return if (castLevel.hasValue()) {
                castLevel.value
            } else {
                MapZoomLevel.One
            }
        }

    var tour: ArticTour? = null
        set(value) {
            field = value
            if (value == null) {
                displayMode.onNext(DisplayMode.General())
            } else {
                displayMode.onNext(DisplayMode.Tour(value))
            }
        }


    init {
        /**
         * TODO:: once integrated to tour flow remove it.
         * Maybe hold the last tour item in the tour commence manager in order to save last state.
         */
        tourDao.getAsyncFirstTour()
                .toObservable()
                .subscribe { tour ->
                    tour?.let {
                        /**
                         * Switch the displayMode to [DisplayMode.Tour] and select the tour floor.
                         */
                        floorChangedTo(tour.floorAsInt)
                        displayMode.onNext(DisplayMode.Tour(tour))
                    }
                }.disposedBy(disposeBag)


        // Each time the floor changes update the current amenities for that floor this is explore mode
        distinctFloor
                .withLatestFrom(displayMode) { floor, mode ->
                    floor to mode
                }.filter { floorToMapDisplayMode -> floorToMapDisplayMode.second is DisplayMode.General }
                .flatMap { floorToMapDisplayMode ->
                    val currentFloor = floorToMapDisplayMode.first
                    mapAnnotationDao.getAmenitiesOnMapForFloor(currentFloor.toString()).toObservable()
                }.map { amenitiesList -> amenitiesList.mapToMapItem() }
                .bindTo(amenities)
                .disposedBy(disposeBag)


        /**
         * Upon changing zoom level to anything except MapZoomLevel.One and any distinct change in
         * floor causes a change of space landmarks
         */
        Observables.combineLatest(
                zoomLevel.distinctUntilChanged()
                        .filter { zoomLevel -> zoomLevel !== MapZoomLevel.One },
                distinctFloor
        ) { _, floor ->
            mapAnnotationDao.getTextAnnotationByTypeAndFloor(
                    ArticMapTextType.SPACE,
                    floor.toString())
                    .toObservable()
                    .map { it.mapToMapItem() }
        }.flatMap { it }
                .bindTo(spacesAndLandmarks)
                .disposedBy(disposeBag)

        displayMode.filter { it is DisplayMode.General }
                .subscribe {
                    setupZoomLevelOneBinds()
                    setupZoomLevelTwoBinds()
                    setupZoomLevelThreeBinds()
                }.disposedBy(disposeBag)

        /**
         * Emits list of tour stops without tour intro.
         */
        val tourObjects: Observable<List<MapItem<*>>> = displayMode
                .filterFlatMap({ it is DisplayMode.Tour }, { it as DisplayMode.Tour })
                .map { mapMode ->
                    mapMode.tour.tourStops.mapNotNull { it.objectId } to mapMode.tour
                }.flatMap { objectIdsWithFloor ->
                    val ids = objectIdsWithFloor.first
                    val tour = objectIdsWithFloor.second
                    objectDao.getObjectsByIdList(ids).toObservable().map {
                        convertToMapItem(it, tour.floorAsInt)
                    }
                }

        /**
         * Combines tour intro and tour stops in order.
         */
        val tourMarkers = displayMode.filterFlatMap({ it is DisplayMode.Tour }, { it as DisplayMode.Tour })
                .map { displayMode ->
                    /** Add tour intro**/
                    listOf<MapItem<*>>(MapItem.TourIntro(displayMode.tour, displayMode.tour.floorAsInt))
                }.zipWith(tourObjects) { introList, stopList ->
                    /** Add tour stops**/
                    mutableListOf<MapItem<*>>().apply {
                        addAll(introList)
                        addAll(stopList)
                    }
                }

        /**
         * Update tour stop markers every time floor is changed.
         */
        Observables
                .combineLatest(distinctFloor, tourMarkers) { _, markers ->
                    markers
                }.bindTo(whatToDisplayOnMap)
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
                .bindTo(centerFullObjectMarker)
                .disposedBy(disposeBag)

    }

    private fun convertToMapItem(it: List<ArticObject>, floor: Int): MutableList<MapItem<ArticObject>> {
        val mapList = mutableListOf<MapItem<ArticObject>>()
        it.forEach { articObject ->
            mapList.add(MapItem.Object(articObject, floor))
        }
        return mapList
    }

    /**
     * Use this to listen to changes in [floor].
     *
     * The returned Observable only emits events if the current zoom is a specific value
     *
     * @param zoomRestriction the desired zoom level
     */
    private fun observeFloorsAtZoom(zoomRestriction: MapZoomLevel): Observable<Int> {
        return Observables.combineLatest(
                zoomLevel,
                distinctFloor
        ) { zoomLevel, floor ->
            return@combineLatest if (zoomLevel === zoomRestriction) floor else Int.MIN_VALUE
        }.filter { floor -> floor >= 0 }
    }


    fun setupZoomLevelOneBinds() {

        /**
         * upon reaching zoom level one load up landmarks
         */
        val zoomLevelOneObservable = observeFloorsAtZoom(MapZoomLevel.One)


        zoomLevelOneObservable
                .flatMap {
                    mapAnnotationDao.getTextAnnotationByType(ArticMapTextType.LANDMARK).toObservable()
                }.map { landmarkList -> landmarkList.mapToMapItem() }
                .bindTo(spacesAndLandmarks)
                .disposedBy(disposeBag)
        zoomLevelOneObservable
                .map {
                    listOf<MapItem<*>>()
                }
                .bindTo(whatToDisplayOnMap)
                .disposedBy(disposeBag)
    }

    fun setupZoomLevelTwoBinds() {

        observeFloorsAtZoom(MapZoomLevel.Two)
                .flatMap { floor ->
                    mapAnnotationDao.getDepartmentOnMapForFloor(floor.toString()).toObservable()
                }.map { it.mapToMapItem() }
                .bindTo(whatToDisplayOnMap)
                .disposedBy(disposeBag)

    }


    fun setupZoomLevelThreeBinds() {
        val galleries = observeGalleriesAtFloor()

        val objects = observeObjectsWithin(galleries)

        Observables.combineLatest(
                galleries,
                objects
        ) { galleryList, objectList ->
            mutableListOf<MapItem<*>>().apply {
                addAll(
                        galleryList.map { gallery ->
                            MapItem.Gallery(gallery, gallery.floorAsInt)
                        }
                )
                addAll(
                        objectList
                )
            }
        }
                // We explicitly permit emission of empty lists, as that is a signal to clear the map.
                .bindTo(whatToDisplayOnMap)
                .disposedBy(disposeBag)
    }

    /**
     * Observe for the Galleries which we're interested in at the moment.
     *
     * The returned [Observable] only emits events if the [current zoom level][zoomLevel]
     * is set to [MapZoomLevel.Three], as that is a pre-requisite for galleries to be
     * displayed on the map.
     */
    fun observeGalleriesAtFloor(): Observable<List<ArticGallery>> {
        return observeFloorsAtZoom(MapZoomLevel.Three)
                .flatMap {
                    galleryDao.getGalleriesForFloor(it.toString()).toObservable()
                }.share()
        // Note: if we don't share this, only one observer could listen to it (we want 2 to do that)
    }

    /**
     * Observe the [ArticObject]s in the given list of galleries.
     *
     * One gallery may contain multiple objects; the returned observable
     * returns all of the objects it finds as a single list. The mechanism
     * we use to retrieve these objects precludes the possibility of
     * duplicates.
     *
     * Each of the observed [MapItem.Object]s include info about the floor
     * of the gallery where it is found.
     */
    fun observeObjectsWithin(observed: Observable<List<ArticGallery>>): Observable<List<MapItem.Object>> {
        return observed.map { galleries ->
            galleries.filter { gallery ->
                gallery.title != null
            }.map { gallery ->
                val title = gallery.title.orEmpty()
                objectDao.getObjectsInGallery(title).map {
                    MapItem.Object(it, gallery.floorAsInt)
                }
            }.flatten()
        }
    }

    fun zoomLevelChangedTo(zoomLevel: MapZoomLevel) {
        this.zoomLevel.onNext(zoomLevel)
    }

    fun floorChangedTo(floor: Int) {
        this.floor.onNext(floor)
    }

    fun departmentMarkerSelected(department: ArticMapAnnotation) {
        this.cameraMovementRequested.onNext(
                Optional(Pair(department.toLatLng(), MapZoomLevel.Three))
        )
        this.cameraMovementRequested.onNext(Optional(null))
    }

    fun articObjectSelected(articObject: ArticObject) {
        selectedArticObject.onNext(articObject)
    }

}