package edu.artic.events

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsLabel
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.models.ArticEvent
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class EventDetailViewModel @Inject constructor(val analyticsTracker: AnalyticsTracker)
    : NavViewViewModel<EventDetailViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class LoadUrl(val url: String) : NavigationEndpoint()
    }

    val imageUrl: Subject<String> = BehaviorSubject.create()
    val title: Subject<String> = BehaviorSubject.createDefault("test")
    val metaData: Subject<String> = BehaviorSubject.createDefault("")
    val description: Subject<String> = BehaviorSubject.createDefault("")
    val throughDate: Subject<String> = BehaviorSubject.createDefault("")
    val location: Subject<String> = BehaviorSubject.createDefault("")
    val eventButtonText: Subject<String> = BehaviorSubject.createDefault("")
    private val eventObservable: Subject<ArticEvent> = BehaviorSubject.create()


    var event: ArticEvent? = null
        set(value) {
            field = value
            value?.let {
                eventObservable.onNext(it)
            }
        }

    init {

        eventObservable
                .map {
                    it.button_text ?: ""
                }
                .bindTo(eventButtonText)
                .disposedBy(disposeBag)

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

    fun onClickRegisterToday() {
        event?.button_url?.let { url ->
            analyticsTracker.reportEvent(ScreenCategoryName.Events, AnalyticsAction.linkPressed, event?.title
                    ?: AnalyticsLabel.Empty)
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.LoadUrl(url)))
        }
    }
}