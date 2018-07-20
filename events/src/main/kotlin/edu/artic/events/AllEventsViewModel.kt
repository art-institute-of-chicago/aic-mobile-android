package edu.artic.events

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.daos.ArticEventDao
import edu.artic.db.models.ArticEvent
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class AllEventsViewModel @Inject constructor(
        eventsDao: ArticEventDao) : BaseViewModel() {

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

}

class AllEventsCellViewModel(tour: ArticEvent) : BaseViewModel() {
    val eventTitle: Subject<String> = BehaviorSubject.createDefault(tour.title)
    val eventDescription: Subject<String> = BehaviorSubject.createDefault(tour.description)
    val eventImageUrl: Subject<String> = BehaviorSubject.createDefault(tour.image?: "")
    //TODO: possible split this into 2 fields
    val eventDateTime: Subject<String> = BehaviorSubject.createDefault(tour.start_at.format(DateTimeHelper.HOME_EVENT_DATE_FORMATTER))
}