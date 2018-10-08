package edu.artic.welcome

import android.arch.lifecycle.LifecycleOwner
import com.fuzz.rx.*
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.base.utils.DateTimeHelper.Purpose.*
import edu.artic.base.utils.orIfNullOrBlank
import edu.artic.db.daos.ArticEventDao
import edu.artic.db.daos.ArticExhibitionDao
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticTour
import edu.artic.localization.LanguageSelector
import edu.artic.membership.MemberInfoPreferencesManager
import edu.artic.viewmodel.CellViewModel
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.Observables
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
                                           private val memberInfoPreferencesManager: MemberInfoPreferencesManager,
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
    val welcomePrompt: Subject<String> = BehaviorSubject.create()
    val currentCardHolder: Subject<String> = BehaviorSubject.create()

    init {
        shouldPeekTourSummary.distinctUntilChanged()
                .subscribe {
                    welcomePreferencesManager.shouldPeekTourSummary = it
                }.disposedBy(disposeBag)

        toursDao.getTourSummary()
                .map {
                    val viewModelList = ArrayList<WelcomeTourCellViewModel>()
                    it.forEach {
                        viewModelList.add(WelcomeTourCellViewModel(disposeBag, it, languageSelector))
                    }
                    return@map viewModelList
                }.bindTo(tours)
                .disposedBy(disposeBag)

        exhibitionDao.getExhibitionSummary()
                .map {
                    val viewModelList = ArrayList<WelcomeExhibitionCellViewModel>()
                    it.forEach {
                        viewModelList.add(WelcomeExhibitionCellViewModel(disposeBag, it, languageSelector))
                    }
                    return@map viewModelList
                }.bindTo(exhibitions)
                .disposedBy(disposeBag)

        eventsDao.getEventSummary()
                .map {
                    val viewModelList = ArrayList<WelcomeEventCellViewModel>()
                    it.forEach {
                        viewModelList.add(WelcomeEventCellViewModel(disposeBag, it, languageSelector))
                    }
                    return@map viewModelList
                }.bindTo(events)
                .disposedBy(disposeBag)


        /**
         * Subscribe to locale change event.
         */
        Observables.combineLatest(
                languageSelector.currentLanguage,
                generalInfoDao.getGeneralInfo().toObservable()
        )
                .map { (_, generalObject) ->
                    languageSelector.selectFrom(generalObject.allTranslations()).homeMemberPromptText
                }.bindTo(welcomePrompt)
                .disposedBy(disposeBag)


    }

    override fun register(lifeCycleOwner: LifecycleOwner) {
        super.register(lifeCycleOwner)
        shouldPeekTourSummary.onNext(welcomePreferencesManager.shouldPeekTourSummary)
    }

    fun updateData() {
        val activeCardHolder = memberInfoPreferencesManager.activeCardHolder
        activeCardHolder?.let { cardHolder ->
            if (cardHolder.isNotEmpty()) {
                currentCardHolder.onNext(activeCardHolder)
            }
        }
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
        analyticsTracker.reportEvent(EventCategoryName.Tour, AnalyticsAction.OPENED, tour.title)
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
 * ViewModel responsible for building each item in the tour summary list.
 */
class WelcomeTourCellViewModel(
        adapterDisposeBag: DisposeBag,
        val tour: ArticTour,
        languageSelector: LanguageSelector
) : CellViewModel(adapterDisposeBag) {

    /**
     * Which translation of [tour] currently matches the
     * [App-level language selection][LanguageSelector.getAppLocale].
     */
    private val tourTranslation: Subject<ArticTour.Translation> = BehaviorSubject.create()

    val tourTitle: Subject<String> = BehaviorSubject.create()
    val tourDescription: Subject<String> = BehaviorSubject.create()
    val tourStops: Subject<String> = BehaviorSubject.createDefault(tour.tourStops.count().toString())
    val tourDuration: Subject<String> = BehaviorSubject.create()
    val tourImageUrl: Subject<String> = BehaviorSubject.createDefault(tour.standardImageUrl)

    init {

        languageSelector.currentLanguage
                .map {
                    languageSelector.selectFrom(tour.allTranslations)
                }
                .bindTo(tourTranslation)
                .disposedBy(disposeBag)

        tourTranslation
                .map {
                    it.description.orIfNullOrBlank(tour.description).orEmpty()
                }
                .bindToMain(tourDescription)
                .disposedBy(disposeBag)

        tourTranslation
                .map {
                    it.title.orIfNullOrBlank(tour.title).orEmpty()
                }
                .bindToMain(tourTitle)
                .disposedBy(disposeBag)

        tourTranslation
                .map {
                    it.tour_duration.orIfNullOrBlank(tour.tourDuration).orEmpty()
                }
                .bindToMain(tourDuration)
                .disposedBy(disposeBag)
    }
}

/**
 * ViewModel responsible for building each item in the `On View` list (i.e. the list of exhibitions).
 */
class WelcomeExhibitionCellViewModel(
        adapterDisposeBag: DisposeBag,
        val exhibition: ArticExhibition,
        val languageSelector: LanguageSelector
) : CellViewModel(adapterDisposeBag) {

    val exhibitionTitleStream: Subject<String> = BehaviorSubject.createDefault(exhibition.title)
    val exhibitionDate: Subject<String> = BehaviorSubject.create()
    val exhibitionImageUrl: Subject<String> = BehaviorSubject.createDefault(exhibition.legacyImageUrl.orEmpty())

    init {

        languageSelector.currentLanguage
                .map {
                    HomeExhibition.obtainFormatter(it)
                }
                .map {
                    exhibition.endTime.format(it)
                }
                .bindToMain(exhibitionDate)
                .disposedBy(disposeBag)

    }

}

/**
 * ViewModel responsible for building the event summary list.
 */
class WelcomeEventCellViewModel(
        adapterDisposeBag: DisposeBag,
        val event: ArticEvent,
        val languageSelector: LanguageSelector
) : CellViewModel(adapterDisposeBag) {

    val eventTitle: Subject<String> = BehaviorSubject.createDefault(event.title)
    val eventShortDescription: Subject<String> = BehaviorSubject.createDefault(event.short_description.orEmpty())
    val eventTime: Subject<String> = BehaviorSubject.create()
    val eventImageUrl: Subject<String> = BehaviorSubject.createDefault(event.imageURL)

    init {
        languageSelector.currentLanguage
                .map {
                    HomeEvent.obtainFormatter(it)
                }
                .map {
                    event.startTime.format(it)
                }
                .bindTo(eventTime)
                .disposedBy(disposeBag)
    }
}