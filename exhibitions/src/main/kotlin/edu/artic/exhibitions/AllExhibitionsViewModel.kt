package edu.artic.exhibitions

import com.fuzz.rx.DisposeBag
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
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
    }

    val exhibitions: Subject<List<AllExhibitionsCellViewModel>> = BehaviorSubject.create()

    init {
        exhibitionsDao.getAllExhibitions()
                .map { list ->
                    val viewModelList = ArrayList<AllExhibitionsCellViewModel>()
                    list.forEach { exhibition ->
                        viewModelList.add(AllExhibitionsCellViewModel(
                                disposeBag,
                                languageSelector,
                                exhibition
                        ))
                    }
                    return@map viewModelList
                }
                .bindTo(exhibitions)
                .disposedBy(disposeBag)
    }

    fun onClickExhibition(position: Int, exhibition: ArticExhibition) {
        analyticsTracker.reportEvent(ScreenCategoryName.Exhibition, AnalyticsAction.OPENED, exhibition.title)
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ExhibitionDetails(position, exhibition)))
    }

}

class AllExhibitionsCellViewModel(
        adapterDisposeBag: DisposeBag,
        val languageSelector: LanguageSelector,
        val exhibition: ArticExhibition
) : CellViewModel(adapterDisposeBag) {

    val exhibitionTitle: Subject<String> = BehaviorSubject.createDefault(exhibition.title)
    val exhibitionEndDate: Subject<String> = BehaviorSubject.create()
    val exhibitionImageUrl: Subject<String> = BehaviorSubject.createDefault(exhibition.legacyImageUrl.orEmpty())

    init {

        languageSelector.currentLanguage
                .map {
                    exhibition.endTime.format(
                            HomeExhibition.obtainFormatter(it)
                    )
                }
                .bindToMain(exhibitionEndDate)
                .disposedBy(disposeBag)
    }

}