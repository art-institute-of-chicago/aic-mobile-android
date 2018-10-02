package edu.artic.events

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.DateTimeHelper.Purpose.*
import edu.artic.db.daos.ArticEventDao
import edu.artic.db.models.ArticEvent
import edu.artic.localization.LanguageSelector
import edu.artic.viewmodel.CellViewModel
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * ViewModel for the 'all events' screen.
 *
 * Each event is given an [AllEventsCellViewModel], and for each set of those that occur
 * on the same date there is one [AllEventsCellHeaderViewModel] immediately preceding.
 */
class AllEventsViewModel @Inject constructor(
        private val analyticsTracker: AnalyticsTracker,
        private val languageSelector: LanguageSelector,
        eventsDao: ArticEventDao
) : NavViewViewModel<AllEventsViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class EventDetail(val pos: Int, val event: ArticEvent) : NavigationEndpoint()
    }

    val events: Subject<List<AllEventsCellBaseViewModel>> = BehaviorSubject.create()

    init {
        eventsDao.getAllEvents()
                .map { list ->
                    val viewModelList = ArrayList<AllEventsCellBaseViewModel>()
                    var prevDayOfMonth: Int = -1
                    var prevMonth: Int = -1
                    var lastHeaderPosition = 0
                    list.forEach { tour ->
                        val startsAt = tour.startTime
                        if (prevMonth != startsAt.monthValue || prevDayOfMonth != startsAt.dayOfMonth) {
                            prevDayOfMonth = startsAt.dayOfMonth
                            prevMonth = startsAt.monthValue
                            viewModelList.add(AllEventsCellHeaderViewModel(languageSelector, tour))
                            lastHeaderPosition = viewModelList.size - 1
                        }
                        viewModelList.add(AllEventsCellViewModel(languageSelector, tour, lastHeaderPosition))
                    }
                    return@map viewModelList
                }
                .bindTo(events)
                .disposedBy(disposeBag)
    }

    fun onClickEvent(pos: Int, event: ArticEvent) {
        analyticsTracker.reportEvent(ScreenCategoryName.Events, AnalyticsAction.OPENED, event.title)
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.EventDetail(pos, event)))
    }

}

open class AllEventsCellBaseViewModel(val event: ArticEvent) : CellViewModel()

class AllEventsCellHeaderViewModel(
        languageSelector: LanguageSelector,
        event: ArticEvent
) : AllEventsCellBaseViewModel(event) {
    val text: Subject<String> = BehaviorSubject.create()


    init {
        languageSelector
                .appLanguageWithUpdates(disposeBag)
                .map {
                    MonthThenDay.obtainFormatter(it)
                }
                .map {
                    event.startTime.format(it)
                }
                .bindTo(text)
                .disposedBy(disposeBag)
    }
}

class AllEventsCellViewModel(
        languageSelector: LanguageSelector,
        event: ArticEvent,
        val headerPosition: Int
) : AllEventsCellBaseViewModel(event) {
    val eventTitle: Subject<String> = BehaviorSubject.createDefault(event.title)
    val eventDescription: Subject<String> = BehaviorSubject.createDefault(event.short_description.orEmpty())
    val eventImageUrl: Subject<String> = BehaviorSubject.createDefault(event.imageURL)
    val eventDateTime: Subject<String> = BehaviorSubject.create()

    init {
        languageSelector
                .appLanguageWithUpdates(disposeBag)
                .map {
                    HomeEvent.obtainFormatter(it)
                }
                .map {
                    event.startTime.format(it)
                }
                .bindTo(eventDateTime)
                .disposedBy(disposeBag)

    }
}