package edu.artic.tours.manager

import android.annotation.SuppressLint
import com.fuzz.rx.Optional
import com.fuzz.rx.filterFlatMap
import edu.artic.db.INTRO_TOUR_STOP_OBJECT_ID
import edu.artic.db.models.ArticTour
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
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

    /**
     * Updates the selected stop when the audio translation playback for a stop completes.
     */
    @SuppressLint("CheckResult")
    fun loadNextTourStop() {
        selectedTour
                .filterFlatMap({ tour -> tour.value != null }, { tour -> tour.value as ArticTour })
                .withLatestFrom(selectedStop)
                .flatMap { (tour, currentStopId) ->
                    tour.tourStops.sortBy { tourStop -> tourStop.order }

                    /**
                     * current stop is guaranteed to be not null
                     */
                    return@flatMap if (currentStopId == INTRO_TOUR_STOP_OBJECT_ID) {
                        Observable.just(tour.tourStops[0].objectId)
                    } else {

                        val currentStop: ArticTour.TourStop = tour.tourStops
                                .find { it.objectId == currentStopId }!!

                        val nextStopIndex = Math.min(tour.tourStops.indexOf(currentStop) + 1, tour.tourStops.size)
                        Observable.just(tour.tourStops[nextStopIndex].objectId)
                    }

                }.subscribe({ tourStopID ->
                    tourStopID?.let {
                        selectedStop.onNext(it)
                    }
                }, { t ->
                    t.printStackTrace()
                })

    }

}