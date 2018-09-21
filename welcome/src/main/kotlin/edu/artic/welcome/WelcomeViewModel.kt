package edu.artic.welcome

import android.arch.lifecycle.LifecycleOwner
import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.daos.ArticEventDao
import edu.artic.db.daos.ArticExhibitionDao
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticTour
import edu.artic.localization.LanguageSelector
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class WelcomeViewModel @Inject constructor(private val welcomePreferencesManager: WelcomePreferencesManager,
                                           private val toursDao: ArticTourDao,
                                           private val eventsDao: ArticEventDao,
                                           private val exhibitionDao: ArticExhibitionDao,
                                           generalInfoDao: GeneralInfoDao,
                                           languageSelector: LanguageSelector,
                                           val analyticsTracker: AnalyticsTracker) : NavViewViewModel<WelcomeViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object Search : NavigationEndpoint()
        object SeeAllTours : NavigationEndpoint()
        object SeeAllOnView : NavigationEndpoint()
        object SeeAllEvents : NavigationEndpoint()
        object AccessMemberCard : NavigationEndpoint()
        class TourDetail(val pos: Int, val tour: ArticTour) : NavigationEndpoint()
        class ExhibitionDetail(val pos: Int, val exhibition: ArticExhibition) : NavigationEndpoint()
        class EventDetail(val pos: Int, val event: ArticEvent) : NavigationEndpoint()
    }


    val shouldPeekTourSummary: Subject<Boolean> = BehaviorSubject.create()
    val tours: Subject<List<WelcomeTourCellViewModel>> = BehaviorSubject.create()
    val exhibitions: Subject<List<WelcomeExhibitionCellViewModel>> = BehaviorSubject.create()
    val events: Subject<List<WelcomeEventCellViewModel>> = BehaviorSubject.create()
    val welcomePrompt : Subject<String> = BehaviorSubject.create()

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

        generalInfoDao
                .getGeneralInfo()
                .map { generalObject ->
                    languageSelector.selectFrom(generalObject.allTranslations()).homeMemberPromptText
                }.bindTo(welcomePrompt)
                .disposedBy(disposeBag)

        /**
         * Subscribe to locale change event.
         */
        languageSelector
                .currentLanguage
                .withLatestFrom(generalInfoDao.getGeneralInfo().toObservable())
                .map { (_, generalObject) ->
                    languageSelector.selectFrom(generalObject.allTranslations()).homeMemberPromptText
                }.bindTo(welcomePrompt)
                .disposedBy(disposeBag)


    }

    override fun register(lifeCycleOwner: LifecycleOwner) {
        super.register(lifeCycleOwner)
        shouldPeekTourSummary.onNext(welcomePreferencesManager.shouldPeekTourSummary)
    }


    fun onPeekedTour() {
        false.asObservable()
                .bindTo(this.shouldPeekTourSummary)
                .disposedBy(disposeBag)
    }

    fun onClickSeeAllTours() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.SeeAllTours))
    }

    fun onClickSeeAllOnView() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.SeeAllOnView))
    }

    fun onClickSeeAllEvents() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.SeeAllEvents))
    }

    fun onClickTour(pos: Int, tour: ArticTour) {
        analyticsTracker.reportEvent(ScreenCategoryName.Tours, AnalyticsAction.OPENED, tour.title)
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.TourDetail(pos, tour)))
    }

    fun onClickExhibition(pos: Int, exhibition: ArticExhibition) {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ExhibitionDetail(pos, exhibition)))
    }

    fun onClickEvent(pos: Int, event: ArticEvent) {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.EventDetail(pos, event)))
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

    fun onAcessMemberCardClickEvent() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.AccessMemberCard))
    }

}

/**
 * ViewModel responsible for building the tour summary list.
 */
class WelcomeTourCellViewModel(val tour: ArticTour) : BaseViewModel() {

    val tourTitle: Subject<String> = BehaviorSubject.createDefault(tour.title)
    val tourDescription: Subject<String> = BehaviorSubject.createDefault(tour.description)
    val tourStops: Subject<String> = BehaviorSubject.createDefault(tour.tourStops.count().toString())
    val tourDuration: Subject<String> = BehaviorSubject.createDefault(tour.tourDuration)
    val tourImageUrl: Subject<String> = BehaviorSubject.createDefault(tour.standardImageUrl)
}

/**
 * ViewModel responsible for building the `On View` list (i.e. list of exhibition).
 */
class WelcomeExhibitionCellViewModel(val exhibition: ArticExhibition) : BaseViewModel() {
    val exhibitionTitleStream: Subject<String> = BehaviorSubject.createDefault(exhibition.title)
    private val throughDateString = exhibition.endTime.format(DateTimeHelper.HOME_EXHIBITION_DATE_FORMATTER)
            .toString()
    val exhibitionDate: Subject<String> = BehaviorSubject.createDefault(throughDateString)
    val exhibitionImageUrl: Subject<String> = BehaviorSubject.createDefault(exhibition.legacy_image_mobile_url.orEmpty())
}

/**
 * ViewModel responsible for building the tour summary list.
 */
class WelcomeEventCellViewModel(val event: ArticEvent) : BaseViewModel() {
    val eventTitle: Subject<String> = BehaviorSubject.createDefault(event.title)
    val eventShortDescription: Subject<String> = BehaviorSubject.createDefault(event.short_description.orEmpty())
    private val eventDate = event.startTime.format(DateTimeHelper.HOME_EVENT_DATE_FORMATTER)
            .toString()
    val eventTime: Subject<String> = BehaviorSubject.createDefault(eventDate)
    val eventImageUrl: Subject<String> = BehaviorSubject.createDefault(event.image.orEmpty())
}