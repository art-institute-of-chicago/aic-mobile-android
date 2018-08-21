package edu.artic.map.carousel

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * Responsible for managing the communication between tour carousel and the map.
 * @author Sameer Dhakal (Fuzz)
 */
class TourProgressManager {
    /**
     * Selected ArticObject
     */
    val selectedStop: Subject<String> = BehaviorSubject.create()

}