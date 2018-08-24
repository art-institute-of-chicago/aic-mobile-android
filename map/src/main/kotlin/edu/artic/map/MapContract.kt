package edu.artic.map

import io.reactivex.Observable


/**
 * @author Sameer Dhakal (Fuzz)
 */
interface MapContract {

    /**
     * Loads map in provided loadMode
     * @param mode
     */
    fun loadMode(mode: MapViewModel.DisplayMode)

    /**
     * Load amenities for map
     * Floor: changes when floor changes.
     * Zoom: N/A
     * Mode: [MapViewModel.DisplayMode.CurrentFloor]
     * Observable<List<MapItem.Annotation>>
     */
    fun getAmenities()

    /**
     * Load spaces and landmarks for map
     * Floor: changes when floor changes.
     * Zoom: Only on higher zoom levels (exclude Zoom Level one]
     * Mode: [MapViewModel.DisplayMode.CurrentFloor, MapViewModel.DisplayMode.Tour]
     */
    fun getSpacesAndLandMarks()

    /**
     * Mode: [MapViewModel.DisplayMode.CurrentFloor]
     * Load all the object markers.
     * Floor : Changes on floor change
     * Zoom  :
     *          first  :  N/A
     *          second :  Only Departments
     *          third  :  Galleries and Objects
     *
     * Mode: [MapViewModel.DisplayMode.Tour]
     * Floor : Changes on floor change
     * Zoom  :
     *        first, second, third : Objects
     *        third : galleries
     */
    fun getMapMarkers()
}