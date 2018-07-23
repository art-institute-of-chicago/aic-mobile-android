package edu.artic.exhibitions

import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.ArticExhibitionDao
import edu.artic.db.models.ArticExhibition
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class ExhibitionDetailViewModel
@Inject
constructor(
        private val exhibitionDao: ArticExhibitionDao,
        dataObjectDao: ArticDataObjectDao)
    : NavViewViewModel<ExhibitionDetailViewModel.NavigationEndpoint>() {
    sealed class NavigationEndpoint {
        class ShowOnMap(val exhibition: ArticExhibition) : NavigationEndpoint()
        class BuyTickets(val url: String) : NavigationEndpoint()
    }

    val title: Subject<String> = BehaviorSubject.createDefault("")
    val metaData: Subject<String> = BehaviorSubject.createDefault("")
    val showOnMapButtonText: Subject<String> = BehaviorSubject.createDefault("Show on Map") // TODO: replace when special localizer is done
    val BuyTicketsButtonText: Subject<String> = BehaviorSubject.createDefault("Buy Tickets")// TODO: replace when special localizer is done
    val description: Subject<String> = BehaviorSubject.createDefault("")
    val throughDate: Subject<String> = BehaviorSubject.createDefault("")

    var ticketsUrl: String? = null
    var exhibition: ArticExhibition? = null

    init {
        dataObjectDao.getDataObject()
                .filter { it.ticketsUrl != null }
                .subscribe {
                    ticketsUrl = it.ticketsUrl!!
                }.disposedBy(disposeBag)
    }

    fun setExhibitionId(exhibition: ArticExhibition) {
        this.exhibition = exhibition

        val exhibitionObservable = this.exhibition.asObservable()
        exhibitionObservable
                .subscribe {
                    this.exhibition = it
                }
                .disposedBy(disposeBag)
        exhibitionObservable
                .map { it.title }
                .bindTo(title)
                .disposedBy(disposeBag)
//        exhibition.map { it.}.bindTo(title).disposedBy(disposeBag) // Meta
        exhibitionObservable
                .filter { it.short_description != null }
                .map { it.short_description!! }
                .bindTo(description)
                .disposedBy(disposeBag)
        exhibitionObservable
                .map { it.aic_end_at.format(DateTimeHelper.HOME_EXHIBITION_DATE_FORMATTER) }
                .bindTo(throughDate)
                .disposedBy(disposeBag)
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