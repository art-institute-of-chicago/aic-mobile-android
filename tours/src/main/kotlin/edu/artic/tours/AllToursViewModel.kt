package edu.artic.tours

import com.fuzz.rx.DisposeBag
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.base.utils.orIfNullOrBlank
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.db.models.ArticTour
import edu.artic.localization.LanguageSelector
import edu.artic.viewmodel.CellViewModel
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class AllToursViewModel @Inject constructor(
        languageSelector: LanguageSelector,
        generalInfoDao: GeneralInfoDao,
        toursDao: ArticTourDao
) : NavViewViewModel<AllToursViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object Search : NavigationEndpoint()
        data class TourDetails(val pos: Int, val tour: ArticTour) : NavigationEndpoint()
    }

    val tours: Subject<List<AllToursCellViewModel>> = BehaviorSubject.create()
    val intro: Subject<String> = BehaviorSubject.createDefault("")

    init {
        toursDao.getTours()
                .map { list ->
                    val viewModelList = ArrayList<AllToursCellViewModel>()
                    list.forEach { tour ->
                        viewModelList.add(AllToursCellViewModel(disposeBag, tour, languageSelector))
                    }
                    return@map viewModelList
                }
                .bindTo(tours)
                .disposedBy(disposeBag)


        /**
         * Subscribe to locale change event.
         */
        Observables.combineLatest(
                languageSelector.currentLanguage,
                generalInfoDao.getGeneralInfo().toObservable()
        )
                .map { (_, generalObject) ->
                    languageSelector.selectFrom(generalObject.allTranslations()).seeAllToursIntro
                }.bindTo(intro)
                .disposedBy(disposeBag)
    }

    fun onClickTour(pos: Int, tour: ArticTour) {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.TourDetails(pos, tour)))
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }
}

/**
 * This class is fundamentally the same as `WelcomeTourCellViewModel` in the :welcome module.
 */
class AllToursCellViewModel(
        adapterDisposeBag: DisposeBag,
        val tour: ArticTour,
        languageSelector: LanguageSelector
) : CellViewModel(adapterDisposeBag) {

    /**
     * Which translation of [tour] currently matches the
     * [App-level language selection][LanguageSelector.getAppLocale].
     */
    private val tourTranslation: Subject<ArticTour.Translation> = BehaviorSubject.create()

    val tourTitle: Subject<String> = BehaviorSubject.create()
    val tourDescription: Subject<String> = BehaviorSubject.create()
    val tourStops: Subject<String> = BehaviorSubject.createDefault(tour.tourStops.count().toString())
    val tourDuration: Subject<String> = BehaviorSubject.create()
    val tourImageUrl: Subject<String> = BehaviorSubject.createDefault(tour.standardImageUrl)

    init {

        languageSelector.currentLanguage
                .map {
                    languageSelector.selectFrom(tour.allTranslations)
                }
                .bindTo(tourTranslation)
                .disposedBy(disposeBag)

        tourTranslation
                .map {
                    it.description.orIfNullOrBlank(tour.description).orEmpty()
                }
                .bindToMain(tourDescription)
                .disposedBy(disposeBag)

        tourTranslation
                .map {
                    it.title.orIfNullOrBlank(tour.title).orEmpty()
                }
                .bindToMain(tourTitle)
                .disposedBy(disposeBag)

        tourTranslation
                .map {
                    it.tour_duration.orIfNullOrBlank(tour.tourDuration).orEmpty()
                }
                .bindToMain(tourDuration)
                .disposedBy(disposeBag)
    }
}