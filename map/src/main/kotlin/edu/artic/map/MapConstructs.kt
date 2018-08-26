package edu.artic.map

import edu.artic.db.models.ArticTour

data class MapChangeEvent(val focus: MapFocus, val floor: Int, val displayMode: MapDisplayMode)

const val ZOOM_LANDMARK: ZoomLevel = 17.5f
const val ZOOM_DEPARTMENTS: ZoomLevel = 18.0f
const val ZOOM_DEPARTMENT_AND_SPACES: ZoomLevel = 19.0f
const val ZOOM_INDIVIDUAL: ZoomLevel = 21.0f

/**
 * Description: Defines at a particular map zoom level, what kind of [MapItem] we are to display
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

typealias ZoomLevel = Float

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
    data class Tour(val tour: ArticTour) : MapDisplayMode()
    object CurrentFloor : MapDisplayMode()
    data class Search<T>(val item: T) : MapDisplayMode()
}