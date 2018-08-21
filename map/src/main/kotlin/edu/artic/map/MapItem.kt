package edu.artic.map

import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour

/**
 * Simple wrapper around DAO entities, intended to simplify the logic within [MapViewModel].
 *
 * Each instance of this class provides data for one marker on the
 * [map][com.google.android.gms.maps.GoogleMap].
 *
 * @param floor the floor of the map where said marker should appear
 */
sealed class MapItem<T>(val item: T, val floor: Int) {
    /**
     * Rich-format label for an area of the map.
     */
    class Annotation(item: ArticMapAnnotation, floor: Int) : MapItem<ArticMapAnnotation>(item, floor)

    /**
     * This acts as a sort of group for multiple [MapItem.Object]s.
     */
    class Gallery(gallery: ArticGallery, floor: Int) : MapItem<ArticGallery>(gallery, floor)

    /**
     * This provides the greatest level of detail - it can represent individual art pieces.
     */
    class Object(item: ArticObject, floor: Int) : MapItem<ArticObject>(item, floor)

    /**
     * This provides detail about the tour object.
     */
    class TourIntro(item: ArticTour, floor: Int) : MapItem<ArticTour>(item, floor)
}