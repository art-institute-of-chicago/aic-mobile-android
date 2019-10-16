package edu.artic.exhibitions

import com.fuzz.rx.DisposeBag
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.localization.util.DateTimeHelper.Purpose.HomeExhibition
import edu.artic.db.daos.ArticExhibitionDao
import edu.artic.db.models.ArticExhibition
import edu.artic.localization.LanguageSelector
import edu.artic.viewmodel.CellViewModel
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class AllExhibitionsViewModel @Inject constructor(
        private val languageSelector: LanguageSelector,
        private val analyticsTracker: AnalyticsTracker,
        exhibitionsDao: ArticExhibitionDao
) : NavViewViewModel<AllExhibitionsViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class ExhibitionDetails(val pos: Int, val exhibition: ArticExhibition) : NavigationEndpoint()
        object Search: NavigationEndpoint()
    }

    val exhibitions: Subject<List<ExhibitionCellViewModel>> = BehaviorSubject.create()

    init {
        exhibitionsDao.getAllExhibitions()
                .map { list ->
                    val viewModelList = ArrayList<ExhibitionCellViewModel>()
                    list.forEach { exhibition ->
                        viewModelList.add(ExhibitionCellViewModel(
                                disposeBag,
                                exhibition,
                                languageSelector
                        ))
                    }
                    return@map viewModelList
                }
                .bindTo(exhibitions)
                .disposedBy(disposeBag)
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

    fun onClickExhibition(position: Int, exhibition: ArticExhibition) {
        analyticsTracker.reportEvent(EventCategoryName.Exhibition, AnalyticsAction.OPENED, exhibition.title)
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ExhibitionDetails(position, exhibition)))
    }

}

/**
 * ViewModel responsible for building each item in the `On View` list (i.e. the list of exhibitions).
 */
class ExhibitionCellViewModel(
        adapterDisposeBag: DisposeBag,
        val exhibition: ArticExhibition,
        val languageSelector: LanguageSelector
) : CellViewModel(adapterDisposeBag) {

    val exhibitionTitle: Subject<String> = BehaviorSubject.createDefault(exhibition.title)
    val exhibitionEndDate: Subject<String> = BehaviorSubject.create()
    val exhibitionImageUrl: Subject<String> = BehaviorSubject.createDefault(exhibition.imageUrl.orEmpty())

    init {

        languageSelector.currentLanguage
                .map {
                    HomeExhibition.obtainFormatter(it)
                }
                .map {
                    exhibition.endTime.format(it)
                }
                .bindToMain(exhibitionEndDate)
                .disposedBy(disposeBag)
    }

}