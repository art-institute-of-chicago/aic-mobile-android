package edu.artic.events

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.daos.ArticEventDao
import edu.artic.db.models.ArticEvent
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class AllEventsViewModel @Inject constructor(
        eventsDao: ArticEventDao, val analyticsTracker: AnalyticsTracker)
    : NavViewViewModel<AllEventsViewModel.NavigationEndpoint>() {

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
                            viewModelList.add(AllEventsCellHeaderViewModel(tour))
                            lastHeaderPosition = viewModelList.size - 1
                        }
                        viewModelList.add(AllEventsCellViewModel(tour, lastHeaderPosition))
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

open class AllEventsCellBaseViewModel(val event: ArticEvent) : BaseViewModel()

class AllEventsCellHeaderViewModel(event: ArticEvent) : AllEventsCellBaseViewModel(event) {
    val text: Subject<String> = BehaviorSubject.createDefault(
            event.startTime.format(DateTimeHelper.MONTH_DAY_FORMATTER)
    )
}

class AllEventsCellViewModel(event: ArticEvent, val headerPosition: Int) : AllEventsCellBaseViewModel(event) {
    val eventTitle: Subject<String> = BehaviorSubject.createDefault(event.title)
    val eventDescription: Subject<String> = BehaviorSubject.createDefault(event.short_description.orEmpty())
    val eventImageUrl: Subject<String> = BehaviorSubject.createDefault(event.image.orEmpty())
    val eventDateTime: Subject<String> = BehaviorSubject.createDefault(
            event.startTime.format(DateTimeHelper.HOME_EVENT_DATE_FORMATTER)
    )
}