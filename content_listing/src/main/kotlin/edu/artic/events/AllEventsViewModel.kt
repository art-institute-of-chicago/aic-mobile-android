package edu.artic.events

import com.fuzz.rx.DisposeBag
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.localization.util.DateTimeHelper.Purpose.*
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
 * Each event is given an [EventCellViewModel], and for each set of those that occur
 * on the same date there is one [AllEventsCellHeaderViewModel] immediately preceding.
 */
class AllEventsViewModel @Inject constructor(
        private val analyticsTracker: AnalyticsTracker,
        private val languageSelector: LanguageSelector,
        eventsDao: ArticEventDao
) : NavViewViewModel<AllEventsViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class EventDetail(val pos: Int, val event: ArticEvent) : NavigationEndpoint()
        object Search : NavigationEndpoint()
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
                            viewModelList.add(AllEventsCellHeaderViewModel(
                                    viewDisposeBag,
                                    languageSelector,
                                    tour
                            ))
                            lastHeaderPosition = viewModelList.size - 1
                        }
                        viewModelList.add(EventCellViewModel(
                                viewDisposeBag,
                                tour,
                                languageSelector,
                                lastHeaderPosition
                        ))
                    }
                    return@map viewModelList
                }
                .bindTo(events)
                .disposedBy(disposeBag)
    }

    fun onClickEvent(pos: Int, event: ArticEvent) {
        analyticsTracker.reportEvent(EventCategoryName.Event, AnalyticsAction.OPENED, event.title)
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.EventDetail(pos, event)))
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

}

open class AllEventsCellBaseViewModel(
        adapterDisposeBag: DisposeBag,
        val event: ArticEvent
) : CellViewModel(adapterDisposeBag)

class AllEventsCellHeaderViewModel(
        adapterDisposeBag: DisposeBag,
        languageSelector: LanguageSelector,
        event: ArticEvent
) : AllEventsCellBaseViewModel(adapterDisposeBag, event) {
    val text: Subject<String> = BehaviorSubject.create()


    init {
        languageSelector
                .currentLanguage
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

/**
 * ViewModel responsible for building the event summary list.
 */
class EventCellViewModel(
        adapterDisposeBag: DisposeBag,
        event: ArticEvent,
        languageSelector: LanguageSelector,
        val headerPosition: Int = -1
) : AllEventsCellBaseViewModel(adapterDisposeBag, event) {

    val eventTitle: Subject<String> = BehaviorSubject.createDefault(event.title)
    val eventDescription: Subject<String> = BehaviorSubject.createDefault(event.short_description.orEmpty())
    val eventImageUrl: Subject<String> = BehaviorSubject.createDefault(event.imageURL)
    val eventDateTime: Subject<String> = BehaviorSubject.create()

    init {
        languageSelector
                .currentLanguage
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