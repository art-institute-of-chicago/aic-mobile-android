package edu.artic.tours

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class TourDetailsViewModel @Inject constructor(private val objectDao: ArticObjectDao) : NavViewViewModel<TourDetailsViewModel.NavigationEndpoint>() {

    val imageUrl: Subject<String> = BehaviorSubject.create()
    val titleText: Subject<String> = BehaviorSubject.create()
    val introductionTitleText: Subject<String> = BehaviorSubject.create()
    val stopsText: Subject<String> = BehaviorSubject.create()
    val timeText: Subject<String> = BehaviorSubject.create()
    //TODO: langaugeSelection
    val startTourButtonText: Subject<String> = BehaviorSubject.createDefault("Start Tour")
    val description: Subject<String> = BehaviorSubject.create()
    val intro: Subject<String> = BehaviorSubject.create()
    val stops: Subject<List<TourDetailsStopCellViewModel>> = BehaviorSubject.create()
    val location: Subject<String> = BehaviorSubject.create()

    private val tourObservable: Subject<ArticTour> = BehaviorSubject.create()

    var tour: ArticTour? = null
        set(value) {
            value?.let { tourObservable.onNext(it) }
        }

    sealed class NavigationEndpoint {
        class Map(tour: ArticTour) : NavigationEndpoint()
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
                .filter { it.description != null }
                .map { it.description!! }
                .bindTo(description)
                .disposedBy(disposeBag)

        tourObservable
                .filter { it.intro != null }
                .map { it.intro!! }
                .bindTo(intro)
                .disposedBy(disposeBag)

        tourObservable
                .map { it.title }
                .bindTo(introductionTitleText)
                .disposedBy(disposeBag)

        tourObservable
                .map { it.tourStops }
                .map { it.first() }
                .filter { it.objectId != null }
                .map { it.objectId!! }
                .subscribe {
                    objectDao.getObjectById(it)
                            .filter { it.galleryLocation != null }
                            .map { it.galleryLocation!! }
                            .bindTo(location)
                            .disposedBy(disposeBag)
                }.disposedBy(disposeBag)


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

    /**
     * Navigate user to the Map activity in Tour Context.
     */
    fun onClickStartTour() {
        tour?.let { tour ->
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Map(tour)))
        }

    }
}

class TourDetailsStopCellViewModel(tourStop: ArticTour.TourStop, objectDao: ArticObjectDao) : BaseViewModel() {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val titleText: Subject<String> = BehaviorSubject.create()
    val galleryText: Subject<String> = BehaviorSubject.create()
    val stopNumber: Subject<String> = BehaviorSubject.createDefault("${tourStop.order + 1}.")

    val articObjectObservable = objectDao.getObjectById(tourStop.objectId.toString())

    init {
        articObjectObservable
                .filter { it.thumbnailFullPath != null }
                .map { it.thumbnailFullPath!! }
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