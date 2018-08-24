package edu.artic.map

import android.util.Log
import com.fuzz.rx.*
import com.google.android.gms.maps.model.LatLng
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapTextType
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.map.carousel.TourProgressManager
import edu.artic.map.helpers.mapToMapItem
import edu.artic.map.helpers.toLatLng
import edu.artic.map.helpers.toMapItem
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
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
        private val tourProgressManager: TourProgressManager
) : BaseViewModel() {


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
        class CurrentFloor() : DisplayMode()
        class Tour(val active: ArticTour) : DisplayMode()
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

    private val zoomLevel: Subject<MapZoomLevel> = BehaviorSubject.createDefault(MapZoomLevel.One)

    val cameraMovementRequested: Subject<Optional<Pair<LatLng, MapZoomLevel>>> = BehaviorSubject.create()
    val leaveTourRequest: Subject<Boolean> = PublishSubject.create()

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


    val displayMode: Subject<DisplayMode> = BehaviorSubject.create()


    /**
     * Load amenities for map
     * Floor: changes when floor changes.
     * Zoom: N/A
     * Mode: [MapViewModel.DisplayMode.CurrentFloor]
     * Observable<List<MapItem.Annotation>>
     */
    private fun loadAmenities() {
        Observables.combineLatest(
                floor.distinctUntilChanged(),
                displayMode.distinctUntilChanged())
        { floor, mode ->
            getAmenitiesFor(floor, mode)
        }.map { amenitiesList ->
            amenitiesList.mapToMapItem()
        }.bindTo(amenities)
    }

    private fun getAmenitiesFor(floor: Int, mode: DisplayMode): List<ArticMapAnnotation> {
        return when (mode) {
            is MapViewModel.DisplayMode.CurrentFloor -> mapAnnotationDao.getAmenitiesOnMapForFloor(floor.toString()).blockingFirst()
            is MapViewModel.DisplayMode.Tour -> emptyList()
        }
    }

    /**
     * Load spaces and landmarks for map
     * Floor: changes when floor changes.
     * Zoom: Only on the [MapZoomLevel.One]
     * Mode: [MapViewModel.DisplayMode.CurrentFloor, MapViewModel.DisplayMode.Tour]
     */
    private fun loadSpacesAndLandMarks() {
        Observables.combineLatest(
                zoomLevel.distinctUntilChanged().filter { zoomLevel -> zoomLevel is MapZoomLevel.One },
                floor.distinctUntilChanged(),
                displayMode.distinctUntilChanged())
        { zoom, floor, mode ->
            getSpacesAndLandMarksFor(floor, mode)
        }.map { it.mapToMapItem() }
                .bindTo(spacesAndLandmarks)
                .disposedBy(disposeBag)
    }

    private fun getSpacesAndLandMarksFor(floor: Int, mode: DisplayMode): List<ArticMapAnnotation> {
        return when (mode) {
            is MapViewModel.DisplayMode.CurrentFloor -> mapAnnotationDao.getTextAnnotationByTypeAndFloor(ArticMapTextType.SPACE, floor.toString()).blockingFirst()
            is MapViewModel.DisplayMode.Tour -> emptyList()
        }
    }

    /**
     * Different behavior for different modes.
     *
     * Mode: [MapViewModel.DisplayMode.CurrentFloor]
     * Load all the object markers.
     * Floor : Changes on floor change
     * Zoom  :
     *          first  :  N/A
     *          second :  Only Departments
     *          third  :  Galleries and Objects
     *
     *
     * Mode: [MapViewModel.DisplayMode.Tour]
     * Floor : Changes on floor change
     * Zoom  :
     *        first, second, third : Objects
     *        third : galleries
     */
    private fun loadMapMarkers() {
        Observables.combineLatest(
                zoomLevel.distinctUntilChanged(),
                floor.distinctUntilChanged(),
                displayMode.distinctUntilChanged()
        ) { zoom, floor, mode ->
            when (mode) {
                is MapViewModel.DisplayMode.CurrentFloor -> {
                    return@combineLatest getMapMarkersOnFloorMode(zoom, floor)
                }
                is MapViewModel.DisplayMode.Tour -> {
                    return@combineLatest getMapMarkersOnTourMode(mode.active, zoom, floor)
                }
            }
        }.bindTo(whatToDisplayOnMap)
                .disposedBy(disposeBag)

    }

    /**
     * Should not run on main thread.
     */
    private fun getMapMarkersOnTourMode(currentTour: ArticTour, zoom: MapZoomLevel?, floor: Int?): List<MapItem<*>> {

        val introTourObject: MapItem<*> = MapItem.TourIntro(currentTour, currentTour.floorAsInt)
        val ids = currentTour.tourStops.mapNotNull { it -> it.objectId }
        val tourObjects = objectDao.getObjectsByIdList(ids).blockingFirst()


        val tourStops: List<MapItem<*>> = convertToMapItem(tourObjects, currentTour.floorAsInt)

        return mutableListOf(introTourObject).apply {
            addAll(tourStops)
        }
    }

    /**
     * Get the markers for current floor mode
     */
    private fun getMapMarkersOnFloorMode(zoom: MapZoomLevel, floor: Int): List<MapItem<*>> {

        return when (zoom) {
            is MapZoomLevel.One -> {
                emptyList()
            }
            is MapZoomLevel.Two -> {
                /**
                 * Load Department on current floor
                 */
                val departmentsOnCurrentFloor = mapAnnotationDao.getDepartmentOnMapForFloor(floor.toString())
                        .blockingFirst()
                        .map { it -> it.toMapItem() }
                departmentsOnCurrentFloor
            }
            is MapZoomLevel.Three -> {
                val markers = mutableListOf<MapItem<*>>()
                /**
                 * Add all galleries.
                 */
                val galleriesForFloor = galleryDao.getGalleriesForFloor(floor.toString()).blockingFirst()
                markers.addAll(galleriesForFloor.map { it -> MapItem.Gallery(it, floor) })

                /**
                 * Add all objects with in the galleries
                 */
                val galleryTitles: List<String> = galleriesForFloor
                        .mapNotNull { it -> it.title }
                val objectsInGalleries = objectDao.getObjectsInGalleries(galleryTitles).blockingFirst()
                        .map { it -> MapItem.Object(it, currentFloor) }
                markers.addAll(objectsInGalleries)
                markers
            }
        }
    }

    val amenities: Subject<List<MapItem.Annotation>> = BehaviorSubject.create()
    val spacesAndLandmarks: Subject<List<MapItem.Annotation>> = BehaviorSubject.create()
    val selectedArticObject: Subject<ArticObject> = BehaviorSubject.create()
    val centerFullObjectMarker: Subject<String> = BehaviorSubject.create()


    init {
        loadAmenities()
        loadSpacesAndLandMarks()
        loadMapMarkers()

        displayMode.filterFlatMap({ it is DisplayMode.Tour }, { it as DisplayMode.Tour })
                .map { it -> it.active }
                .mapOptional()
                .bindTo(tourProgressManager.selectedTour)
                .disposedBy(disposeBag)



        tourProgressManager
                .leaveTourRequest
                .bindTo(leaveTourRequest)
                .disposedBy(disposeBag)

        /**
         * Reset tour object
         */
        tourProgressManager
                .selectedTour
                .distinctUntilChanged()
                .subscribe { selectedTour ->
                    val tour = selectedTour.value
                    if (tour == null) {
                        loadCurrentFloorMode()
                    } else {
                        loadTourMode(tour)
                    }

                }.disposedBy(disposeBag)


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

        tourProgressManager
                .selectedTour
                .skip(1)
                .subscribe { value ->
                    val tour = value.value
                    if (tour == null) {
                        Log.d("LEAVE", "leaving tour")
                    }
                }.disposedBy(disposeBag)
    }

    private fun loadCurrentFloorMode() {
        displayMode.onNext(DisplayMode.CurrentFloor())
        floorChangedTo(1)
    }

    fun loadTourMode(tour: ArticTour) {
        displayMode.onNext(DisplayMode.Tour(tour))
        floorChangedTo(tour.floorAsInt)
    }


    private fun convertToMapItem(it: List<ArticObject>, floor: Int): MutableList<MapItem<ArticObject>> {
        val mapList = mutableListOf<MapItem<ArticObject>>()
        it.forEach { articObject ->
            mapList.add(MapItem.Object(articObject, floor))
        }
        return mapList
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

    fun leaveTour() {
        tourProgressManager.selectedTour.onNext(Optional(null))
    }

}