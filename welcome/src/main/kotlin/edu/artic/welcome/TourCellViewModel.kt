package edu.artic.welcome

import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class TourCellViewModel(tour: ArticTour) : BaseViewModel() {

    val tourTitle: Subject<String> = BehaviorSubject.createDefault(tour.title)
    val tourDescription: Subject<String> = BehaviorSubject.createDefault(tour.description)
    private val tourStopCount = tour.tourStops?.count() ?: 0
    val tourStops: Subject<String> = BehaviorSubject.createDefault(tourStopCount.toString())
    val tourDuration: Subject<String> = BehaviorSubject.createDefault(tour.tourDuration)
    val tourImageUrl: Subject<String> = BehaviorSubject.createDefault(tour.imageUrl)

}