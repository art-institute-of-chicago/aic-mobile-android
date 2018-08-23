package edu.artic.map

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.map.helpers.toLatLng

/**
 * Simple wrapper around DAO entities, intended to simplify the logic within [MapViewModel].
 *
 * Each instance of this class provides data for one marker on the
 * [map][com.google.android.gms.maps.GoogleMap].
 *
 * @param floor the floor of the map where said marker should appear
 */
sealed class MapItem<T>(open val item: T, open val floor: Int) {
    /**
     * Rich-format label for an area of the map.
     */
    data class Annotation(override val item: ArticMapAnnotation, override val floor: Int) : MapItem<ArticMapAnnotation>(item, floor)

    /**
     * This acts as a sort of group for multiple [MapItem.Object]s.
     */
    data class Gallery(override val item: ArticGallery, override val floor: Int) : MapItem<ArticGallery>(item, floor)

    /**
     * This provides the greatest level of detail - it can represent individual art pieces.
     *
     * There are usually an order of magnitude more [Object]s than [Gallery]s.
     */
    data class Object(override val item: ArticObject, override val floor: Int) : MapItem<ArticObject>(item, floor)

    /**
     * This provides detail about the tour object.
     */
    data class TourIntro(override val item: ArticTour, override val floor: Int) : MapItem<ArticTour>(item, floor)

    /**
     * TODO: This function is currently unused. Perhaps convert to abstract method?
     */
    fun toLatLng() : LatLng {
        return when {
            this is Annotation -> item.toLatLng()
            this is Gallery -> item.toLatLng()
            this is Object -> item.toLatLng()
            else -> throw IllegalStateException()
        }
    }
}