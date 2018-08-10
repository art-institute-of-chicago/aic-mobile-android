package edu.artic.map

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.google.android.gms.maps.model.LatLng
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapTextType
import edu.artic.map.helpers.mapToMapItem
import edu.artic.map.helpers.toLatLng
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class MapViewModel @Inject constructor(
        private val mapAnnotationDao: ArticMapAnnotationDao,
        private val galleryDao: ArticGalleryDao,
        private val objectDao: ArticObjectDao
) : BaseViewModel() {

    val amenities: Subject<List<MapItem.Annotation>> = BehaviorSubject.create()
    val spacesAndLandmarks: Subject<List<MapItem.Annotation>> = BehaviorSubject.create()

    //This stores all the map items that change at every zoom level and every floor change
    val veryDynamicMapItems: Subject<List<MapItem<*>>> = BehaviorSubject.create()


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


    fun setupZoomLevelOneBinds() {

        /**
         * upon reaching zoom level one load up landmarks
         */
        val zoomLevelOneObservable = zoomLevel.distinctUntilChanged()
                .filter { zoomLevel -> zoomLevel === MapZoomLevel.One }


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
                .bindTo(veryDynamicMapItems)
                .disposedBy(disposeBag)
    }

    fun setupZoomLevelTwoBinds() {

        Observables.combineLatest(
                zoomLevel,
                distinctFloor
        ) { zoomLevel, floor ->
            return@combineLatest if (zoomLevel === MapZoomLevel.Two) floor else Int.MIN_VALUE
        }.filter { floor -> floor >= 0 }
                .flatMap { floor ->
                    mapAnnotationDao.getDepartmentOnMapForFloor(floor.toString()).toObservable()
                }.map { it.mapToMapItem() }
                .bindTo(veryDynamicMapItems)
                .disposedBy(disposeBag)

    }


    fun setupZoomLevelThreeBinds() {
        val galleries = Observables.combineLatest(
                zoomLevel,
                distinctFloor
        ) { zoomLevel, floor ->
            return@combineLatest if (zoomLevel === MapZoomLevel.Three) floor else Int.MIN_VALUE
        }.filter { floor -> floor >= 0 }
                .flatMap {
                    galleryDao.getGalleriesForFloor(it.toString()).toObservable()
                }

        val objects = galleries
                .map { galleryList ->
                    galleryList.filter { it.titleT != null }.map { it.titleT.orEmpty() }
                }.flatMap {
                    objectDao.getObjectsInGalleries(it).toObservable()
                }

        Observables.combineLatest(
                distinctFloor,
                galleries,
                objects
        ) { floor, galleryList, objectList ->
            return@combineLatest if (currentZoomLevel == MapZoomLevel.Three && floor == currentFloor) {
                val list = mutableListOf<MapItem<*>>()
                list.addAll(
                        galleryList.map { gallery ->
                            MapItem.Gallery(gallery, floor)
                        }
                )
                list.addAll(
                        objectList.map { articObject ->
                            MapItem.Object(articObject, floor)
                        }
                )
                list
            } else {
                emptyList<MapItem<*>>()
            }
        }.filter { it.isNotEmpty() }
                .bindTo(veryDynamicMapItems)
                .disposedBy(disposeBag)
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

}