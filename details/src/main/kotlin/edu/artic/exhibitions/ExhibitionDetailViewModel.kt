package edu.artic.exhibitions

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsLabel
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.models.ArticExhibition
import edu.artic.details.R
import edu.artic.localization.LanguageSelector
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class ExhibitionDetailViewModel
@Inject
constructor(dataObjectDao: ArticDataObjectDao,
            val galleryDao: ArticGalleryDao,
            val analyticsTracker: AnalyticsTracker,
            languageSelector: LanguageSelector
) : NavViewViewModel<ExhibitionDetailViewModel.NavigationEndpoint>() {
    sealed class NavigationEndpoint {
        class ShowOnMap(val exhibition: ArticExhibition) : NavigationEndpoint()
        class BuyTickets(val url: String) : NavigationEndpoint()
    }

    val imageUrl: Subject<String> = BehaviorSubject.create()
    val title: Subject<String> = BehaviorSubject.createDefault("test")
    val metaData: Subject<String> = BehaviorSubject.createDefault("")
    val showOnMapButtonText: Subject<Int> = BehaviorSubject.createDefault(R.string.showOnMap)
    val buyTicketsButtonText: Subject<Int> = BehaviorSubject.createDefault(R.string.buyTickets)
    val description: Subject<String> = BehaviorSubject.createDefault("")
    val throughDate: Subject<String> = BehaviorSubject.createDefault("")
    /**
     * Pair of `latitude` to `longitude`.
     *
     * Either latitude or longitude may be null, but this always has a value
     * if the [exhibition] is non-null.
     */
    val location: Subject<Pair<Double?, Double?>> = BehaviorSubject.create()
    private val exhibitionObservable: Subject<ArticExhibition> = BehaviorSubject.create()


    var ticketsUrl: String? = null

    var exhibition: ArticExhibition? = null
        set(value) {
            field = value
            value?.let { exhibitionObservable.onNext(it) }
        }

    init {
        dataObjectDao.getDataObject()
                .map {it.ticketsUrlAndroid }
                .filter { it.isNotEmpty() }
                .subscribe {
                    ticketsUrl = it
                }.disposedBy(disposeBag)
        exhibitionObservable
                .map { it.title }
                .bindTo(title)
                .disposedBy(disposeBag)

        exhibitionObservable
                .filterFlatMap({ it.legacyImageUrl != null }, { it.legacyImageUrl!! })
                .bindTo(imageUrl)
                .disposedBy(disposeBag)

        exhibitionObservable
                .map { it.latitude to it.longitude }
                .bindTo(location)
                .disposedBy(disposeBag)

        exhibitionObservable
                .filter { it.short_description != null }
                .map { it.short_description!! }
                .bindTo(description)
                .disposedBy(disposeBag)

        exhibitionObservable
                .map { exhibition ->
                    val formatter = DateTimeHelper
                            .obtainFormatter(
                                    DateTimeHelper.Purpose.HomeExhibition,
                                    languageSelector.getAppLocale()
                            )
                    exhibition.endTime.format(formatter)
                }
                .bindTo(throughDate)
                .disposedBy(disposeBag)

        languageSelector.currentLanguage
                .withLatestFrom(exhibitionObservable)
                .map { (locale, exhibition) ->
                    exhibition.endTime.format(DateTimeHelper
                            .obtainFormatter(
                                    DateTimeHelper.Purpose.HomeExhibition,
                                    locale
                            )
                    )
                }.bindTo(throughDate)
                .disposedBy(disposeBag)
    }

    fun onClickShowOnMap() {
        exhibition?.let { exhibition ->
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ShowOnMap(exhibition)))
        }
    }

    fun onClickBuyTickets() {
        ticketsUrl?.let { url ->
            analyticsTracker.reportEvent(ScreenCategoryName.Exhibition, AnalyticsAction.linkPressed, exhibition?.title
                    ?: AnalyticsLabel.Empty)
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.BuyTickets(url)))
        }
    }


}