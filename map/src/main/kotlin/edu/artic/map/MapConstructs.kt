package edu.artic.map

import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.db.models.ArticTour

data class MapChangeEvent(val focus: MapFocus, val floor: Int, val displayMode: MapDisplayMode)

/**
 * A more fluent naming that describes a map zoom level. Analogous to [Float] but when Kotlin gets
 * inline classes, this'll provide more safety.
 */
typealias ZoomLevel = Float

const val ZOOM_LANDMARK: ZoomLevel = 17.5f
const val ZOOM_DEPARTMENTS: ZoomLevel = 18.0f
const val ZOOM_DEPARTMENT_AND_SPACES: ZoomLevel = 19.0f
const val ZOOM_INDIVIDUAL: ZoomLevel = 20.3f
const val ZOOM_MAX: ZoomLevel = 20.999999f
const val ZOOM_MIN: ZoomLevel = 17.0f

/**
 * Represents a range of [ZoomLevel] that we display marker map items at. Some markers traverse
 * multiple zoom levels.
 */
enum class MapFocus {
    Landmark,
    Department,
    DepartmentAndSpaces,
    Individual;

    fun toZoomLevel(): ZoomLevel = when (this) {
        Landmark -> ZOOM_LANDMARK
        Department -> ZOOM_DEPARTMENTS
        DepartmentAndSpaces -> ZOOM_DEPARTMENT_AND_SPACES
        Individual -> ZOOM_INDIVIDUAL
    }
}


/**
 * Convers a [ZoomLevel] int into a
 */
internal fun ZoomLevel.toMapFocus(): MapFocus = when {
    this <= ZOOM_LANDMARK -> MapFocus.Landmark
    this <= ZOOM_DEPARTMENTS -> MapFocus.Department
    this <= ZOOM_DEPARTMENT_AND_SPACES -> MapFocus.DepartmentAndSpaces
    else -> MapFocus.Individual
}

sealed class MapDisplayMode {
    data class Tour(val tour: ArticTour, val selectedTourStop: ArticTour.TourStop?) : MapDisplayMode()
    object CurrentFloor : MapDisplayMode()
    sealed class Search<T>(val item: T) : MapDisplayMode() {
        class ObjectSearch(item: ArticSearchArtworkObject) : Search<ArticSearchArtworkObject>(item)
        class AmenitiesSearch(amenityType: String) : Search<String>(amenityType)
        class ExhibitionSearch(item: ArticExhibition) : Search<ArticExhibition>(item)
    }
}