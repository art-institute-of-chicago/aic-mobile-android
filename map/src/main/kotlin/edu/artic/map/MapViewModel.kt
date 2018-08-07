package edu.artic.map

import com.fuzz.rx.Optional
import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.google.android.gms.maps.model.LatLng
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapAnnotationType
import edu.artic.db.models.ArticMapTextType
import edu.artic.map.helpers.mapToMapItem
import edu.artic.map.helpers.toLatLng
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject

class MapViewModel @Inject constructor(
        private val mapAnnotationDao: ArticMapAnnotationDao,
        private val galleryDao: ArticGalleryDao,
        private val objectDao: ArticObjectDao
) : BaseViewModel() {

    val mapAnnotations: Subject<List<MapItem<*>>> = BehaviorSubject.create()

    val alwaysVisibleAnnotations: Subject<List<MapItem.Annotation>> = BehaviorSubject.create()

    val floor: Subject<Int> = BehaviorSubject.createDefault(1)
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
        val allAmenities = mapAnnotationDao
                .getAmenitiesOnMap()
                .map { annotationList ->
                    annotationList.mapToMapItem()
                }

        val allBuildingNames = mapAnnotationDao
                .getBuildingNamesOnMap()
                .map { annotationList ->
                    annotationList.mapToMapItem()
                }

        Observables.combineLatest(allAmenities.asObservable(), allBuildingNames.asObservable())
        { amenities, buildingNames ->
            return@combineLatest amenities.toMutableList() + buildingNames
        }.bindTo(alwaysVisibleAnnotations)
                .disposedBy(disposeBag)

        setupZoomLevelOneBinds()
        setupZoomLevelTwoBinds()
        setupZoomLevelThreeBinds()

    }


    fun setupZoomLevelOneBinds() {
        Observables.combineLatest(
                zoomLevel.distinctUntilChanged().filter { it === MapZoomLevel.One },
                floor.distinctUntilChanged(),
                alwaysVisibleAnnotations.map { annotationList ->
                    annotationList.filter { item ->
                        item.item.annotationType == ArticMapAnnotationType.AMENITY ||
                                (item.item.annotationType == ArticMapAnnotationType.TEXT &&
                                        item.item.textType == ArticMapTextType.LANDMARK)
                    }
                }
        ) { _: MapZoomLevel, floor: Int, annotations: List<MapItem.Annotation> ->
            annotations.filter { item ->
                (item.item.annotationType == ArticMapAnnotationType.AMENITY && item.floor == floor)
                        || item.item.annotationType == ArticMapAnnotationType.TEXT

            }
        }.bindTo(mapAnnotations)
                .disposedBy(disposeBag)
    }

    fun setupZoomLevelTwoBinds() {
        Observables.combineLatest(
                zoomLevel.distinctUntilChanged().filter { it === MapZoomLevel.Two },
                floor.distinctUntilChanged(),
                alwaysVisibleAnnotations.map { itemList ->
                    itemList.filter { item ->
                        item.item.annotationType == ArticMapAnnotationType.AMENITY ||
                                (item.item.annotationType == ArticMapAnnotationType.TEXT &&
                                        item.item.textType == ArticMapTextType.SPACE)
                    }
                },
                mapAnnotationDao
                        .getDepartmentOnMap()
                        .asObservable()
                        .map { departmentList ->
                            departmentList.mapToMapItem()
                        }
        ) { _, floor, annotations, deparments ->
            annotations.filter { it.floor == floor }.toMutableList() +
                    deparments.filter { it.item.floor?.toInt() == floor }
        }.bindTo(mapAnnotations)
                .disposedBy(disposeBag)
    }


    fun setupZoomLevelThreeBinds() {
        val galleries = Observables.combineLatest(
                zoomLevel.distinctUntilChanged().filter { it === MapZoomLevel.Three },
                floor.distinctUntilChanged())
        { _, floor ->
            floor
        }.flatMap {
            galleryDao.getGalleriesForFloor(it.toString()).asObservable()
        }

        val objects = galleries
                .map { galleryList ->
                    galleryList.filter { it.titleT != null }.map { it.titleT.orEmpty() }
                }.flatMap {
                    objectDao.getObjectsInGalleries(it).asObservable()
                }

        Observables.combineLatest(
                floor,
                galleries,
                objects,
                alwaysVisibleAnnotations.map { itemList ->
                    itemList.filter { item ->
                        item.item.annotationType == ArticMapAnnotationType.AMENITY ||
                                (item.item.annotationType == ArticMapAnnotationType.TEXT &&
                                        item.item.textType == ArticMapTextType.SPACE)
                    }
                }
        ) { floor, galleryList, objectList, annotations ->
            val list = mutableListOf<MapItem<*>>()
            list.addAll(annotations.filter { it.floor == floor })
            list.addAll(galleryList.map { gallery -> MapItem.Gallery(gallery, floor) })
            list.addAll(objectList.map{ articObject -> MapItem.Object(articObject, floor) })
            return@combineLatest list
        }.bindTo(mapAnnotations)
                .disposedBy(disposeBag)
    }

    fun zoomLevelChangedTo(zoomLevel: MapZoomLevel) {
        this.zoomLevel.onNext(zoomLevel)
    }

    fun floorChangedTo(floor: Int) {
        this.floor.onNext(floor)
    }

    fun departmentMarkerSelected(department: ArticMapAnnotation){
        this.cameraMovementRequested.onNext(
                Optional(Pair(department.toLatLng(), MapZoomLevel.Three))
        )
    }

}