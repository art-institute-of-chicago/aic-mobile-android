package edu.artic.map

import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticObject
import edu.artic.map.helpers.mapToMapItem
import edu.artic.map.helpers.toMapItem
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.functions.Function4
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
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
                        item.item.annotationType == "Amenity" ||
                                (item.item.annotationType == "Text" && item.item.textType == "Landmark")
                    }
                }
        ) {
            _: MapZoomLevel, floor: Int, annotations: List<MapItem.Annotation> ->
            annotations.filter { item ->
                (item.item.annotationType == "Amenity" && item.floor == floor)
                        || item.item.annotationType == "Text"

            }
        }.bindTo(mapAnnotations)
                .disposedBy(disposeBag)
    }

    fun setupZoomLevelTwoBinds() {
        Observable.combineLatest(
                zoomLevel.distinctUntilChanged().filter { it === MapZoomLevel.Two },
                floor.distinctUntilChanged(),
                alwaysVisibleAnnotations.map { itemList ->
                    itemList.filter { item ->
                        item.item.annotationType == "Amenity" ||
                                (item.item.annotationType == "Text" && item.item.textType == "Space")
                    }
                },
                mapAnnotationDao
                        .getDepartmentOnMap()
                        .asObservable()
                        .map { departmentList ->
                            val list = mutableListOf<MapItem.Annotation>()
                            departmentList.forEach { department ->
                                val floor = department.floor
                                        .let {
                                            it?.toInt() ?: 1
                                        }
                                list.add(MapItem.Annotation(department, floor))
                            }
                            return@map list

                        },
                Function4<MapZoomLevel, Int, List<MapItem.Annotation>, List<MapItem.Annotation>, List<MapItem<*>>>
                { _, floor, annotations, deparments ->
                    val list = mutableListOf<MapItem<*>>()
                    list.addAll(annotations.filter { it.floor == floor })
                    list.addAll(deparments.filter { it.item.floor?.toInt() == floor })
                    return@Function4 list
                }).bindTo(mapAnnotations)
                .disposedBy(disposeBag)
    }


    fun setupZoomLevelThreeBinds() {
        val galleries = Observable.combineLatest(
                zoomLevel.distinctUntilChanged().filter { it === MapZoomLevel.Three },
                floor.distinctUntilChanged(),
                BiFunction<MapZoomLevel, Int, Int> { _, floor ->
                    return@BiFunction floor
                })
                .flatMap {
                    return@flatMap galleryDao.getGalleriesForFloor(it.toString()).asObservable()
                }
        val objects = galleries
                .map {
                    it.filter { it.titleT != null }.map { it.titleT!! }
                }.flatMap {
                    objectDao.getObjectsInGalleries(it).asObservable()
                }

        Observable.combineLatest(
                floor,
                galleries,
                objects,
                alwaysVisibleAnnotations.map { itemList ->
                    itemList.filter { item ->
                        item.item.annotationType == "Amenity" ||
                                (item.item.annotationType == "Text" && item.item.textType == "Space")
                    }
                },
                Function4<Int, List<ArticGallery>, List<ArticObject>, List<MapItem.Annotation>, List<MapItem<*>>>
                { floor, galleryList, objectList, annotations ->
                    val list = mutableListOf<MapItem<*>>()
                    list.addAll(annotations.filter { it.floor == floor })
                    galleryList.forEach { gallery ->
                        list.add(MapItem.Gallery(gallery, floor))
                    }
                    objectList.forEach { articObject ->
                        list.add(MapItem.Object(articObject, floor))
                    }
                    return@Function4 list
                })
                .bindTo(mapAnnotations)
                .disposedBy(disposeBag)
    }

    fun zoomLevelChangedTo(zoomLevel: MapZoomLevel) {
        this.zoomLevel.onNext(zoomLevel)
    }

    fun floorChangedTo(floor: Int) {
        this.floor.onNext(floor)
    }


}