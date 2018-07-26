package edu.artic.events

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
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
        eventsDao: ArticEventDao) : NavViewViewModel<AllEventsViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class EventDetail(val pos: Int, val event: ArticEvent) : NavigationEndpoint()
    }

    val events: Subject<List<AllEventsCellViewModel>> = BehaviorSubject.create()

    init {
        eventsDao.getAllEvents()
                .map { list ->
                    val viewModelList = ArrayList<AllEventsCellViewModel>()
                    list.forEach { tour ->
                        viewModelList.add(AllEventsCellViewModel(tour))
                    }
                    return@map viewModelList
                }
                .bindTo(events)
                .disposedBy(disposeBag)
    }

    fun onClickEvent(pos : Int, event: ArticEvent) {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.EventDetail(pos, event)))
    }

}

class AllEventsCellViewModel(val event: ArticEvent) : BaseViewModel() {
    val eventTitle: Subject<String> = BehaviorSubject.createDefault(event.title)
    val eventDescription: Subject<String> = BehaviorSubject.createDefault(event.short_description ?: "")
    val eventImageUrl: Subject<String> = BehaviorSubject.createDefault(event.image?: "")
    //TODO: possible split this into 2 fields
    val eventDateTime: Subject<String> = BehaviorSubject.createDefault(event.start_at.format(DateTimeHelper.HOME_EVENT_DATE_FORMATTER))
}