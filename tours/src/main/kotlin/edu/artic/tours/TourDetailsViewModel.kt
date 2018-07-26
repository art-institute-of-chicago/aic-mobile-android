package edu.artic.tours

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class TourDetailsViewModel @Inject constructor(private val objectDao: ArticObjectDao) : BaseViewModel() {

    val imageUrl: Subject<String> = BehaviorSubject.create()
    val titleText: Subject<String> = BehaviorSubject.create()
    val stopsText: Subject<String> = BehaviorSubject.create()
    val timeText: Subject<String> = BehaviorSubject.create()
    //TODO: langaugeSelection
    val startTourButtonText: Subject<String> = BehaviorSubject.createDefault("Start Tour")
    val description: Subject<String> = BehaviorSubject.create()
    val stops: Subject<List<TourDetailsStopCellViewModel>> = BehaviorSubject.create()

    private val tourObservable: Subject<ArticTour> = BehaviorSubject.create()

    var tour: ArticTour? = null
        set(value) {
            value?.let { tourObservable.onNext(it) }
        }

    init {
        tourObservable
                .map { it.title }
                .bindTo(titleText)
                .disposedBy(disposeBag)

        tourObservable
                .filter { it.imageUrl != null }
                .map { it.imageUrl!! }
                .bindTo(imageUrl)
                .disposedBy(disposeBag)

        //TODO: replace Stops with localized string when localizer is created
        tourObservable
                .map { "${it.tourStops.count()} Stops" }
                .bindTo(stopsText)
                .disposedBy(disposeBag)

        tourObservable
                .filter { it.tourDuration != null }
                .map { it.tourDuration!! } // TODO: possibly add mins at end
                .bindTo(timeText)
                .disposedBy(disposeBag)

        tourObservable
                .filter { it.descriptionHtml != null }
                .map { it.descriptionHtml!! }
                .bindTo(description)


        tourObservable
                .map { it.tourStops }
                .observeOn(Schedulers.io())
                .map {
                    val list = mutableListOf<TourDetailsStopCellViewModel>()
                    it.forEach { tourStop ->
                        list.add(TourDetailsStopCellViewModel(tourStop, objectDao))
                    }
                    return@map list
                }.bindTo(stops)
                .disposedBy(disposeBag)

    }

    fun onClickStartTour() {

    }
}

class TourDetailsStopCellViewModel(tourStop: ArticTour.TourStop, objectDao: ArticObjectDao) : BaseViewModel() {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val titleText: Subject<String> = BehaviorSubject.create()
    val galleryText: Subject<String> = BehaviorSubject.create()
    val stopNumber: Subject<String> = BehaviorSubject.createDefault(tourStop.order.toString())

    private val articObjectObservable = objectDao.getObjectById(tourStop.objectId.toString())

    init {
        articObjectObservable
                .filter { it.image_url != null }
                .map {
                    it.image_url!!
                }
                .bindTo(imageUrl)
                .disposedBy(disposeBag)
        articObjectObservable
                .map { it.title }
                .bindTo(titleText)
                .disposedBy(disposeBag)
        articObjectObservable
                .filter { it.galleryLocation != null }
                .map { it.galleryLocation!! }
                .bindTo(galleryText)
    }
}