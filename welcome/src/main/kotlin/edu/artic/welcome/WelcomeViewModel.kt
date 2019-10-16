package edu.artic.welcome

import android.arch.lifecycle.LifecycleOwner
import com.fuzz.rx.*
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.db.daos.ArticEventDao
import edu.artic.db.daos.ArticExhibitionDao
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticTour
import edu.artic.events.EventCellViewModel
import edu.artic.exhibitions.ExhibitionCellViewModel
import edu.artic.localization.LanguageSelector
import edu.artic.membership.MemberInfoPreferencesManager
import edu.artic.tours.TourCellViewModel
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
    val tours: Subject<List<TourCellViewModel>> = BehaviorSubject.create()
    val exhibitions: Subject<List<ExhibitionCellViewModel>> = BehaviorSubject.create()
    val events: Subject<List<EventCellViewModel>> = BehaviorSubject.create()
    val welcomePrompt: Subject<String> = BehaviorSubject.create()
    val currentCardHolder: Subject<String> = BehaviorSubject.create()

    init {
        shouldPeekTourSummary.distinctUntilChanged()
                .subscribe {
                    welcomePreferencesManager.shouldPeekTourSummary = it
                }.disposedBy(disposeBag)

        toursDao.getTourSummary()
                .map {
                    val viewModelList = ArrayList<TourCellViewModel>()
                    it.forEach {
                        viewModelList.add(TourCellViewModel(disposeBag, it, languageSelector))
                    }
                    return@map viewModelList
                }.bindTo(tours)
                .disposedBy(disposeBag)

        exhibitionDao.getExhibitionSummary()
                .map {
                    val viewModelList = ArrayList<ExhibitionCellViewModel>()
                    it.forEach {
                        viewModelList.add(ExhibitionCellViewModel(disposeBag, it, languageSelector))
                    }
                    return@map viewModelList
                }.bindTo(exhibitions)
                .disposedBy(disposeBag)

        eventsDao.getEventSummary()
                .map {
                    val viewModelList = ArrayList<EventCellViewModel>()
                    it.forEach {
                        viewModelList.add(EventCellViewModel(disposeBag, it, languageSelector))
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


