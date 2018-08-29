package edu.artic.map

/**
 * Description: Defines at a particular map zoom level, what kind of [MapItem] we are to display
 */
enum class MapFocus {
    Landmark,
    Department,
    DepartmentAndSpaces,
    Individual
}