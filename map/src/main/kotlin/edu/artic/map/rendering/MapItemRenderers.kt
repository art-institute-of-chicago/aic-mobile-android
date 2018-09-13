package edu.artic.map.rendering

import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import com.bumptech.glide.Glide
import com.fuzz.rx.asFlowable
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticMapAmenityType
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapTextType
import edu.artic.image.asRequestObservable
import edu.artic.image.toBitmap
import edu.artic.map.*
import edu.artic.map.helpers.toLatLng
import io.reactivex.Flowable
import io.reactivex.Observable

internal const val ALPHA_DIMMED = 0.6f
internal const val ALPHA_VISIBLE = 1.0f


/**
 * Common logic for non-tour objects.
 * If active in search, then always display. We assume when you get to this point, the item should show on the map,
 * and in search mode that is only one item at a time.
 * If on current tour, don't show.
 * @param allContentFocus A lazy evaluated method that only gets run if the we're in [MapDisplayMode.CurrentFloor]
 * @param displayMode The current map's [MapDisplayMode]
 * @return a [Set] of [MapFocus] indicating which markers to show.
 */
internal inline fun searchMapFocus(displayMode: MapDisplayMode, allContentFocus: () -> Set<MapFocus>): Set<MapFocus> =
        when (displayMode) {
            is MapDisplayMode.Tour -> setOf() // don't show
            is MapDisplayMode.Search<*> -> MapFocus.values().toSet() // assume visible if we get here.
            is MapDisplayMode.CurrentFloor -> allContentFocus()
        }

/**
 * Convenience construct for handling [ArticMapAnnotation] typed objects.
 */
abstract class MapAnnotationItemRenderer(protected val articMapAnnotationDao: ArticMapAnnotationDao,
                                         useBitmapQueue: Boolean = false)
    : MapItemRenderer<ArticMapAnnotation>(useBitmapQueue) {
    override fun getLocationFromItem(item: ArticMapAnnotation): LatLng = item.toLatLng()

    override fun getIdFromItem(item: ArticMapAnnotation): String = item.nid
}

/**
 * Displays Landmark items.
 */
class LandmarkMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val textMarkerGenerator: TextMarkerGenerator by lazy { TextMarkerGenerator(context) }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticMapAnnotation>> {
        return when (displayMode) {
            is MapDisplayMode.Search<*> ->
                listOf<ArticMapAnnotation>().asFlowable()
            else ->
                articMapAnnotationDao.getTextAnnotationByType(ArticMapTextType.LANDMARK)
        }
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            searchMapFocus(displayMode) { setOf(MapFocus.Landmark) }

    override fun getFastBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty()))

    override val zIndex: Float = ALPHA_VISIBLE
}

class SpacesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao)
    : MapAnnotationItemRenderer(articMapAnnotationDao) {

    private val textMarkerGenerator: TextMarkerGenerator  by lazy { TextMarkerGenerator(context) }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticMapAnnotation>> {
        return when (displayMode) {
            is MapDisplayMode.Search<*> ->
                listOf<ArticMapAnnotation>().asFlowable()
            else ->
                articMapAnnotationDao.getTextAnnotationByTypeAndFloor(ArticMapTextType.SPACE, floor = floor.toString()) // TODO: switch to int
        }
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            searchMapFocus(displayMode) { setOf(MapFocus.DepartmentAndSpaces, MapFocus.Individual) }

    override fun getFastBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.label.orEmpty()))

    override val zIndex: Float = ALPHA_VISIBLE
}

class AmenitiesMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao) : MapAnnotationItemRenderer(articMapAnnotationDao) {
    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticMapAnnotation>> {
        return when (displayMode) {
            is MapDisplayMode.Search.ObjectSearch ->
                listOf<ArticMapAnnotation>().asFlowable()
            is MapDisplayMode.Search.AmenitiesSearch -> {
                val amenityType = ArticMapAmenityType.getAmenityTypes(displayMode.item)
                if (displayMode.item == ArticMapAmenityType.DINING) {
                    articMapAnnotationDao.getAmenitiesByAmenityType(amenityType = amenityType)
                } else {
                    articMapAnnotationDao.getAmenitiesByAmenityType(floor = floor.toString(), amenityType = amenityType)
                }
            }
            is MapDisplayMode.Tour ->{
                /**
                 * Only display restrooms on tour mode.
                 */
                val amenityTypes = ArticMapAmenityType.getAmenityTypes(ArticMapAmenityType.RESTROOMS)
                articMapAnnotationDao.getAmenitiesByAmenityType(floor = floor.toString(), amenityType = amenityTypes)
            }
            else -> articMapAnnotationDao.getAmenitiesOnMapForFloor(floor = floor.toString())
        }
    }

    override fun getMarkerAlpha(floor: Int, mapDisplayMode: MapDisplayMode, item: ArticMapAnnotation): Float {
        return if (mapDisplayMode is MapDisplayMode.Search.AmenitiesSearch) {
            if (item.floor == floor) ALPHA_VISIBLE else ALPHA_DIMMED
        } else {
            ALPHA_VISIBLE
        }
    }


    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> = MapFocus.values().toSet() // all zoom levels

    override fun getFastBitmap(item: ArticMapAnnotation, displayMode: MapDisplayMode): BitmapDescriptor {
        return BitmapDescriptorFactory.fromResource(amenityIconForAmenityType(item.amenityType))
    }

    override val zIndex: Float = 0.0f
}

class DepartmentsMapItemRenderer(articMapAnnotationDao: ArticMapAnnotationDao)
    : MapAnnotationItemRenderer(articMapAnnotationDao, useBitmapQueue = true) {

    private val departmentMarkerGenerator: DepartmentMarkerGenerator by lazy { DepartmentMarkerGenerator(context) }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticMapAnnotation>> {
        return when (displayMode) {
            is MapDisplayMode.Search<*> ->
                listOf<ArticMapAnnotation>().asFlowable()
            else ->
                articMapAnnotationDao.getDepartmentOnMapForFloor(floor = floor.toString())
        }
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            searchMapFocus(displayMode) { setOf(MapFocus.Department, MapFocus.DepartmentAndSpaces) }

    override fun getBitmapFetcher(item: ArticMapAnnotation, displayMode: MapDisplayMode): Observable<BitmapDescriptor>? {
        return Glide.with(context)
                .asBitmap()
                .load(item.standardImageUrl)
                .asRequestObservable(context)
                .map { BitmapDescriptorFactory.fromBitmap(departmentMarkerGenerator.makeIcon(it, item.label.orEmpty())) }
    }

    override val zIndex: Float = 2.0f
}

class GalleriesMapItemRenderer(private val galleriesDao: ArticGalleryDao)
    : MapItemRenderer<ArticGallery>() {

    private val textMarkerGenerator: TextMarkerGenerator by lazy { TextMarkerGenerator(context) }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticGallery>> {
        return when (displayMode) {
            is MapDisplayMode.Search<*> ->
                listOf<ArticGallery>().asFlowable()
            else ->
                galleriesDao.getGalleriesForFloor(floor = floor.toString())
        }
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            searchMapFocus(displayMode) { setOf(MapFocus.Individual) }

    override fun getLocationFromItem(item: ArticGallery): LatLng = item.toLatLng()

    override fun getFastBitmap(item: ArticGallery, displayMode: MapDisplayMode): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(textMarkerGenerator.makeIcon(item.displayTitle))

    override fun getIdFromItem(item: ArticGallery): String = item.galleryId.orEmpty()

    override val zIndex: Float = ALPHA_VISIBLE
}


/**
 * Simple [Pair]-like object which holds a [LatLng] and [Int] icon.
 */
data class LionMapItem(val location: LatLng, @DrawableRes val iconId: Int)

class LionMapItemRenderer : MapItemRenderer<LionMapItem>() {

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            MapFocus.values().toSet()

    override val zIndex: Float
        get() = 1.0f

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<LionMapItem>> {
        return Flowable.just(listOf(
                LionMapItem(LatLng(41.879491568164525, -87.624089977901931), R.drawable.map_lion_1),
                LionMapItem(LatLng(41.879678006591391, -87.624091248446064), R.drawable.map_lion_2)))
    }

    override fun getLocationFromItem(item: LionMapItem): LatLng = item.location

    override fun getIdFromItem(item: LionMapItem): String = item.iconId.toString()

    override fun getFastBitmap(item: LionMapItem, displayMode: MapDisplayMode): BitmapDescriptor? {
        return BitmapDescriptorFactory.fromBitmap(
                ContextCompat.getDrawable(context, item.iconId)!!.toBitmap())
    }
}