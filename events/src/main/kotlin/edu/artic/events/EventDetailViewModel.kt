package edu.artic.events

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.models.ArticEvent
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class EventDetailViewModel @Inject constructor(): BaseViewModel() {

    val imageUrl: Subject<String> = BehaviorSubject.create()
    val title: Subject<String> = BehaviorSubject.createDefault("test")
    val metaData: Subject<String> = BehaviorSubject.createDefault("")
    val description: Subject<String> = BehaviorSubject.createDefault("")
    val throughDate: Subject<String> = BehaviorSubject.createDefault("")
    val location: Subject<String> = BehaviorSubject.createDefault("")

    var event: ArticEvent? = null
        set(value) {
            field = value
            val eventObservable = BehaviorSubject.createDefault(value)

            eventObservable
                    .map { it.title }
                    .bindTo(title)
                    .disposedBy(disposeBag)

            eventObservable
                    .filter { it.image != null }
                    .map { it.image!! }
                    .bindTo(imageUrl)
                    .disposedBy(disposeBag)

            eventObservable
                    .map { it.start_at.format(DateTimeHelper.HOME_EVENT_DATE_FORMATTER) }
                    .bindTo(metaData)
                    .disposedBy(disposeBag)

            eventObservable
                    .filter { it.description != null }
                    .map { it.description!! }
                    .bindTo(description)
                    .disposedBy(disposeBag)

            eventObservable
                    .map { "Through ${it.end_at.format(DateTimeHelper.HOME_EXHIBITION_DATE_FORMATTER)}" }
                    .bindTo(throughDate)
                    .disposedBy(disposeBag)

            eventObservable
                    .filter { it.location != null }
                    .map { it.location!! }
                    .bindTo(location)
                    .disposedBy(disposeBag)
        }
}