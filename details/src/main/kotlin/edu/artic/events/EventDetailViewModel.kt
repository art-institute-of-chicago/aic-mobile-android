package edu.artic.events

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.db.models.ArticEvent
import edu.artic.localization.LanguageSelector
import edu.artic.localization.util.DateTimeHelper.Purpose.HomeEvent
import edu.artic.localization.util.DateTimeHelper.Purpose.HomeExhibition
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

class EventDetailViewModel @Inject constructor(
        val analyticsTracker: AnalyticsTracker,
        languageSelector: LanguageSelector
) : NavViewViewModel<EventDetailViewModel.NavigationEndpoint>() {

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
    val hasEventUrl: Subject<Boolean> = BehaviorSubject.create()
    val isOnSale: Subject<Boolean> = BehaviorSubject.create()
    val buttonCaptionText: Subject<String?> = BehaviorSubject.createDefault("")
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
                    it.button_text.orEmpty()
                }
                .bindTo(eventButtonText)
                .disposedBy(disposeBag)

        eventObservable
                .map { !it.button_url.isNullOrBlank() }
                .bindTo(hasEventUrl)
                .disposedBy(disposeBag)

        eventObservable
                .map { it.title }
                .bindTo(title)
                .disposedBy(disposeBag)

        eventObservable
                .map { it.imageURL }
                .bindTo(imageUrl)
                .disposedBy(disposeBag)

        Observables.combineLatest(
                languageSelector.currentLanguage,
                eventObservable
        )
                .map { (locale, event) ->
                    event.startTime.format(
                            HomeEvent.obtainFormatter(locale)
                    )
                }
                .bindTo(metaData)
                .disposedBy(disposeBag)

        eventObservable
                .filter { it.description != null }
                .map { it.description!! }
                .bindTo(description)
                .disposedBy(disposeBag)


        /**
         * Listen for language changes.
         */
        Observables.combineLatest(
                languageSelector.currentLanguage,
                eventObservable
        )
                .map { (locale, event) ->
                    val formatter = HomeExhibition.obtainFormatter(locale)
                    event.endTime.format(formatter)
                }
                .bindTo(throughDate)
                .disposedBy(disposeBag)

        eventObservable
                .filter { it.location != null }
                .map { it.location!! }
                .bindTo(location)
                .disposedBy(disposeBag)

        eventObservable
                .map {
                    it.isTicketed && it.isSalesButtonHidden != true
                            && currentlyInSaleWindow(it.onSaleAt, it.offSaleAt)
                }
                .bindTo(isOnSale)
                .disposedBy(disposeBag)

        eventObservable
                .map { it.buttonCaption.orEmpty() }
                .bindTo(buttonCaptionText)
                .disposedBy(disposeBag)
    }

    private fun currentlyInSaleWindow(onSaleAt: ZonedDateTime?, offSaleAt: ZonedDateTime?) : Boolean {
        val currentTime = ZonedDateTime.now()
        return when {
            onSaleAt == null -> true
            currentTime.isBefore(onSaleAt) -> false
            offSaleAt != null && currentTime.isAfter(offSaleAt) -> false
            else -> true
        }
    }

    fun onClickRegisterToday() {
        event?.let {
            analyticsTracker.reportEvent(EventCategoryName.Event, AnalyticsAction.linkPressed, it.title)
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.LoadUrl(it.buttonURL)))
        }
    }
}