package edu.artic.welcome

import android.arch.lifecycle.LifecycleOwner
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class WelcomeViewModel @Inject constructor(private val welcomePreferencesManager: WelcomePreferencesManager) : BaseViewModel() {

    val shouldPeekTourSummary: Subject<Boolean> = BehaviorSubject.create()
    val tours: Subject<List<WelcomeTourCellViewModel>> = BehaviorSubject.create()
    val exhibitions: Subject<List<WelcomeExhibitionCellViewModel>> = BehaviorSubject.create()
    val events: Subject<List<WelcomeEventCellViewModel>> = BehaviorSubject.create()

    init {
        shouldPeekTourSummary.distinctUntilChanged()
                .subscribe {
                    welcomePreferencesManager.shouldPeekTourSummary = it
                }.disposedBy(disposeBag)
    }

    override fun register(lifeCycleOwner: LifecycleOwner) {
        super.register(lifeCycleOwner)
        shouldPeekTourSummary.onNext(welcomePreferencesManager.shouldPeekTourSummary)
    }


    fun onPeekedTour() {
        Observable.just(false)
                .bindTo(this.shouldPeekTourSummary)
                .disposedBy(disposeBag)
    }

    /**
     * Temp method until dao is ready
     * TODO:: fetch first 6 tours from db.
     */
    fun addTours(tours: List<ArticTour>) {
        val viewModelList = ArrayList<WelcomeTourCellViewModel>()
        tours.forEach {
            viewModelList.add(WelcomeTourCellViewModel(it))
        }
        this.tours.onNext(viewModelList)
    }

    /**
     * Temp method to until dao is ready
     * TODO:: fetch first 6 exhibitions from db.
     */
    fun addExhibitions(exhibitions: List<ArticExhibition>) {
        val viewModelList = ArrayList<WelcomeExhibitionCellViewModel>()
        exhibitions.forEach {
            viewModelList.add(WelcomeExhibitionCellViewModel(it))
        }
        this.exhibitions.onNext(viewModelList)
    }

    /**
     * Temp method to until dao is ready
     * TODO:: fetch first 6 events from db.
     */
    fun addEvents(events: List<ArticEvent>) {
        val viewModelList = ArrayList<WelcomeEventCellViewModel>()
        events.forEach {
            viewModelList.add(WelcomeEventCellViewModel(it))
        }
        this.events.onNext(viewModelList)
    }
}

/**
 * ViewModel responsible for building the tour summary list.
 */
class WelcomeTourCellViewModel(tour: ArticTour) : BaseViewModel() {

    val tourTitle: Subject<String> = BehaviorSubject.createDefault(tour.title)
    val tourDescription: Subject<String> = BehaviorSubject.createDefault(tour.description)
    private val tourStopCount = tour.tourStops?.count() ?: 0
    val tourStops: Subject<String> = BehaviorSubject.createDefault(tourStopCount.toString())
    val tourDuration: Subject<String> = BehaviorSubject.createDefault(tour.tourDuration)
    val tourImageUrl: Subject<String> = BehaviorSubject.createDefault(tour.imageUrl)
}

/**
 * ViewModel responsible for building the `On View` list (i.e. list of exhibition).
 */
class WelcomeExhibitionCellViewModel(exhibition: ArticExhibition) : BaseViewModel() {
    val exhibitionTitleStream: Subject<String> = BehaviorSubject.createDefault(exhibition.title)
    val exhibitionDate: Subject<String> = BehaviorSubject.createDefault("2017 August 9")
    val exhibitionImageUrl: Subject<String> = BehaviorSubject.createDefault(exhibition.legacy_image_mobile_url
            ?: "")
}

/**
 * ViewModel responsible for building the tour summary list.
 */
class WelcomeEventCellViewModel(event: ArticEvent) : BaseViewModel() {
    val eventTitle: Subject<String> = BehaviorSubject.createDefault(event.title)
    val eventShortDescription: Subject<String> = BehaviorSubject.createDefault(event.short_description
            ?: "")
    val eventTime: Subject<String> = BehaviorSubject.createDefault(event.start_at)
    val eventImageUrl: Subject<String> = BehaviorSubject.createDefault(event.image ?: "")
}