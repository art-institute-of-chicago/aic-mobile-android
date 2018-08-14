package edu.artic.map

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.google.android.gms.maps.model.LatLng
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapTextType
import edu.artic.db.models.ArticObject
import edu.artic.map.helpers.mapToMapItem
import edu.artic.map.helpers.toLatLng
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
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
        private val objectDao: ArticObjectDao
) : BaseViewModel() {

    val amenities: Subject<List<MapItem.Annotation>> = BehaviorSubject.create()
    val spacesAndLandmarks: Subject<List<MapItem.Annotation>> = BehaviorSubject.create()
    val selectedArticObject: Subject<ArticObject> = BehaviorSubject.create()

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

    init {

        // Each time the floor changes update the current amenities for that floor this is explore mode
        distinctFloor
                .flatMap { floor ->
                    mapAnnotationDao.getAmenitiesOnMapForFloor(floor.toString()).toObservable()
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

        setupZoomLevelOneBinds()
        setupZoomLevelTwoBinds()
        setupZoomLevelThreeBinds()

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
        }.filter { it.isNotEmpty() }
                .bindTo(whatToDisplayOnMap)
                .disposedBy(disposeBag)
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
                gallery.titleT != null
            }.map { gallery ->
                val title = gallery.titleT.orEmpty()
                objectDao.getObjectsInGallery(title).map {
                    MapItem.Object(it, gallery.floorAsInt)
                }
            }.flatten()
        }
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