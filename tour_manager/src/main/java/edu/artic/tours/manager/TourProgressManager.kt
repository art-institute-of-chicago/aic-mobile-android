package edu.artic.tours.manager

import com.fuzz.rx.Optional
import edu.artic.db.models.ArticTour
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
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
    /**
     * Save the last selected tour.
     */
    val selectedTour: Subject<Optional<ArticTour>> = BehaviorSubject.createDefault(Optional(null))
    val proposedTour: Subject<Optional<Pair<ArticTour, ArticTour.TourStop>>> = BehaviorSubject.createDefault(Optional(null))
    val leaveTourRequest: Subject<Boolean> = PublishSubject.create()
}