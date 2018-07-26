package edu.artic.tours

import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class TourDetailsViewModel @Inject constructor() : BaseViewModel() {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val titleText: Subject<String> = BehaviorSubject.create()
    val stopsText: Subject<String> = BehaviorSubject.create()
    val timeText: Subject<String> = BehaviorSubject.create()
    //TODO: langaugeSelection
    val startTourButtonText: Subject<String> = BehaviorSubject.createDefault("Start Tour")
    val description: Subject<String> = BehaviorSubject.create()
    val stops: Subject<List<TourDetailsStopCellViewModel>> = BehaviorSubject.create()

    val tourObservable : Subject<ArticTour> = BehaviorSubject.create()

    var tour: ArticTour? = null
        set(value) {
            value?.let { tourObservable.onNext(it) }
        }
}

class TourDetailsStopCellViewModel() : BaseViewModel() {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val titleText: Subject<String> = BehaviorSubject.create()
    val galleryText: Subject<String> = BehaviorSubject.create()
    val stopNumber: Subject<String> = BehaviorSubject.create()
}