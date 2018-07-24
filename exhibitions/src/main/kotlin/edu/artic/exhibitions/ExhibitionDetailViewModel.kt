package edu.artic.exhibitions

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.models.ArticExhibition
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class ExhibitionDetailViewModel
@Inject
constructor(dataObjectDao: ArticDataObjectDao)
    : NavViewViewModel<ExhibitionDetailViewModel.NavigationEndpoint>() {
    sealed class NavigationEndpoint {
        class ShowOnMap(val exhibition: ArticExhibition) : NavigationEndpoint()
        class BuyTickets(val url: String) : NavigationEndpoint()
    }

    val imageUrl: Subject<String> = BehaviorSubject.create()
    val title: Subject<String> = BehaviorSubject.createDefault("test")
    val metaData: Subject<String> = BehaviorSubject.createDefault("")
    val showOnMapButtonText: Subject<String> = BehaviorSubject.createDefault("Show on Map") // TODO: replace when special localizer is done
    val buyTicketsButtonText: Subject<String> = BehaviorSubject.createDefault("Buy Tickets")// TODO: replace when special localizer is done
    val description: Subject<String> = BehaviorSubject.createDefault("")
    val throughDate: Subject<String> = BehaviorSubject.createDefault("")

    var ticketsUrl: String? = null

    var exhibition: ArticExhibition? = null
    set(value) {
        field = value
        val exhibitionObservable = BehaviorSubject.createDefault(exhibition)

        exhibitionObservable
                .map { it.title }
                .bindTo(title)
                .disposedBy(disposeBag)

        exhibitionObservable
                .filter { it.legacy_image_mobile_url != null }
                .map { it.legacy_image_mobile_url!! }

                .bindTo(imageUrl)
                .disposedBy(disposeBag)

        exhibitionObservable
                .doOnError {
                    it.printStackTrace()
                }
                .map { it.aic_start_at.format(DateTimeHelper.HOME_EVENT_DATE_FORMATTER) }
                .bindTo(metaData)
                .disposedBy(disposeBag)

        exhibitionObservable
                .filter { it.short_description != null }
                .map { it.short_description!! }
                .bindTo(description)
                .disposedBy(disposeBag)

        exhibitionObservable
                .map { "Through ${it.aic_end_at.format(DateTimeHelper.HOME_EXHIBITION_DATE_FORMATTER)}" }
                .bindTo(throughDate)
                .disposedBy(disposeBag)
    }

    init {
        dataObjectDao.getDataObject()
                .filter { it.ticketsUrl != null }
                .map { it.ticketsUrl!! }
                .subscribe {
                    ticketsUrl = it
                }.disposedBy(disposeBag)
    }

    fun onClickShowOnMap() {
        exhibition?.let { exhibition ->
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ShowOnMap(exhibition)))
        }
    }

    fun onClickBuyTickets() {
        ticketsUrl?.let { url ->
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.BuyTickets(url)))
        }
    }


}