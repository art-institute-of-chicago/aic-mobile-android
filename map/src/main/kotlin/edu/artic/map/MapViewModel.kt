package edu.artic.map

import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class MapViewModel @Inject constructor(private val mapAnnotationDao: ArticMapAnnotationDao) : BaseViewModel() {

    val mapAnnotations: Subject<List<MapItem>> = BehaviorSubject.create()

    val allAmenities: Subject<List<MapItem.MapAnnotation>> = BehaviorSubject.create()
    val allBuildingNames: Subject<List<MapItem.MapAnnotation>> = BehaviorSubject.create()

    val floor: Subject<Int> = BehaviorSubject.createDefault(1)
    val zoomLevel: Subject<MapZoomLevel> = BehaviorSubject.create()

    init {

        mapAnnotationDao
                .getAmenitiesOnMap()
                .map { annotationList ->
                    val list = mutableListOf<MapItem.MapAnnotation>()
                    annotationList.forEach { annotation ->
                        list.add(MapItem.MapAnnotation(annotation))
                    }
                    return@map list

                }
                .bindTo(allAmenities)
                .disposedBy(disposeBag)

        mapAnnotationDao
                .getBuildingNamesOnMap()
                .map { annotationList ->
                    val list = mutableListOf<MapItem.MapAnnotation>()
                    annotationList.forEach { annotation ->
                        list.add(MapItem.MapAnnotation(annotation))
                    }
                    return@map list

                }.bindTo(allBuildingNames)

        Observable.combineLatest<Int, MapZoomLevel, Pair<Int, MapZoomLevel>>(
                floor.distinctUntilChanged(),
                zoomLevel.distinctUntilChanged(),
                BiFunction<Int, MapZoomLevel, Pair<Int, MapZoomLevel>> { floor, zoomLevel ->
                    Pair(floor, zoomLevel)
                }
        ).subscribe { (floor, zoomLevel) ->
            when (zoomLevel) {
                MapZoomLevel.One -> {
                    loadZoomLevelOneForFloor(floor)
                }
                MapZoomLevel.Two -> {
                    loadZoomLevelTwoForFloor(floor)
                }
                MapZoomLevel.Three -> {
                    loadZoomLevelThreeForFloor(floor)
                }
            }
        }.disposedBy(disposeBag)
    }

    fun zoomLevelChangedTo(zoomLevel: MapZoomLevel) {
        this.zoomLevel.onNext(zoomLevel)
    }

    fun floorChangedTo(floor: Int) {
        this.floor.onNext(floor)
    }

    fun loadZoomLevelOneForFloor(floor: Int) {
        Observable.combineLatest(
                allBuildingNames
                        .map { it.filter { it.annotation.floor?.toInt() == floor } },
                allAmenities
                        .map { it.filter { it.annotation.floor?.toInt() == floor } },
                BiFunction<List<MapItem.MapAnnotation>, List<MapItem.MapAnnotation>, List<MapItem>>
                { buildingList, mapItemList ->
                    val list = mutableListOf<MapItem>()
                    list.addAll(mapItemList)
                    list.addAll(buildingList)
                    return@BiFunction list
                })
                .bindTo(mapAnnotations)
                .disposedBy(disposeBag)
    }

    fun loadZoomLevelTwoForFloor(floor: Int) {
        Observable.combineLatest(
                allBuildingNames
                        .map { buildingList ->
                            buildingList.filter { building ->
                                building.annotation.floor?.toInt() == floor
                            }
                        },
                allAmenities
                        .map { amenitiesList ->
                            amenitiesList.filter { amenity ->
                                amenity.annotation.floor?.toInt() == floor
                            }
                        },
                mapAnnotationDao
                        .getDepartmentOnMap()
                        .asObservable()
                        .map { departmentList ->
                            departmentList.filter { deparment ->
                                deparment.floor?.toInt() == floor
                            }
                        }.map {annotationList ->
                            val list = mutableListOf<MapItem.MapAnnotation>()
                            annotationList.forEach { annotation ->
                                list.add(MapItem.MapAnnotation(annotation))
                            }
                            return@map list
                        },
                Function3<List<MapItem>, List<MapItem>, List<MapItem>, List<MapItem>>
                { buildingList, amenityList, departmentList ->
                    val list = mutableListOf<MapItem>()
                    list.addAll(buildingList)
                    list.addAll(amenityList)
                    list.addAll(departmentList)

                    return@Function3 list
                })
                .bindTo(mapAnnotations)
                .disposedBy(disposeBag)
    }

    fun loadZoomLevelThreeForFloor(floor: Int) {
        mapAnnotations.onNext(listOf())
    }

}

sealed class MapItem {
    class MapAnnotation(val annotation: ArticMapAnnotation) : MapItem()
}