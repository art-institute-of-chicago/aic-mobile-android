package edu.artic.welcome

import android.arch.lifecycle.LifecycleOwner
import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.daos.ArticEventDao
import edu.artic.db.daos.ArticExhibitionDao
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.threeten.bp.LocalDateTime
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class WelcomeViewModel @Inject constructor(private val welcomePreferencesManager: WelcomePreferencesManager,
                                           private val toursDao: ArticTourDao,
                                           private val eventsDao: ArticEventDao,
                                           private val exhibitionDao: ArticExhibitionDao) : NavViewViewModel<WelcomeViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint{
        class SeeAllTours : NavigationEndpoint()
    }


    val shouldPeekTourSummary: Subject<Boolean> = BehaviorSubject.create()
    val tours: Subject<List<WelcomeTourCellViewModel>> = BehaviorSubject.create()
    val exhibitions: Subject<List<WelcomeExhibitionCellViewModel>> = BehaviorSubject.create()
    val events: Subject<List<WelcomeEventCellViewModel>> = BehaviorSubject.create()

    init {
        shouldPeekTourSummary.distinctUntilChanged()
                .subscribe {
                    welcomePreferencesManager.shouldPeekTourSummary = it
                }.disposedBy(disposeBag)

        toursDao.getTourSummary()
                .map {
                    val viewModelList = ArrayList<WelcomeTourCellViewModel>()
                    it.forEach {
                        viewModelList.add(WelcomeTourCellViewModel(it))
                    }
                    return@map viewModelList
                }.bindTo(tours)
                .disposedBy(disposeBag)

        exhibitionDao.getExhibitionSummary()
                .map {
                    val viewModelList = ArrayList<WelcomeExhibitionCellViewModel>()
                    it.forEach {
                        viewModelList.add(WelcomeExhibitionCellViewModel(it))
                    }
                    return@map viewModelList
                }.bindTo(exhibitions)
                .disposedBy(disposeBag)

        eventsDao.getEventSummary()
                .map {
                    val viewModelList = ArrayList<WelcomeEventCellViewModel>()
                    it.forEach {
                        viewModelList.add(WelcomeEventCellViewModel(it))
                    }
                    return@map viewModelList
                }.bindTo(events)
                .disposedBy(disposeBag)


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

    fun onClickSeeAllTours() {
        Navigate.Forward(NavigationEndpoint.SeeAllTours())
                .asObservable()
                .bindTo(navigateTo)
                .disposedBy(disposeBag)
    }
}

/**
 * ViewModel responsible for building the tour summary list.
 */
class WelcomeTourCellViewModel(tour: ArticTour) : BaseViewModel() {

    val tourTitle: Subject<String> = BehaviorSubject.createDefault(tour.title)
    val tourDescription: Subject<String> = BehaviorSubject.createDefault(tour.description)
    private val tourStopCount = tour.tourStops.count() ?: 0
    val tourStops: Subject<String> = BehaviorSubject.createDefault(tourStopCount.toString())
    val tourDuration: Subject<String> = BehaviorSubject.createDefault(tour.tourDuration)
    val tourImageUrl: Subject<String> = BehaviorSubject.createDefault(tour.imageUrl)
}

/**
 * ViewModel responsible for building the `On View` list (i.e. list of exhibition).
 */
class WelcomeExhibitionCellViewModel(exhibition: ArticExhibition) : BaseViewModel() {
    val exhibitionTitleStream: Subject<String> = BehaviorSubject.createDefault(exhibition.title)
    private val throughDate: LocalDateTime = LocalDateTime.parse(exhibition.aic_end_at, DateTimeHelper.DEFAULT_FORMATTER)
    private val throughDateString = throughDate.format(DateTimeHelper.HOME_EXHIBITION_DATE_FORMATTER)
            .toString()
    val exhibitionDate: Subject<String> = BehaviorSubject.createDefault(throughDateString)
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
    private val eventDate = event.start_at.format(DateTimeHelper.HOME_EVENT_DATE_FORMATTER)
            .toString()
    val eventTime: Subject<String> = BehaviorSubject.createDefault(eventDate)
    val eventImageUrl: Subject<String> = BehaviorSubject.createDefault(event.image ?: "")
}