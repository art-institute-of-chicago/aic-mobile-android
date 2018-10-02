package edu.artic.tours

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
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
        toursDao: ArticTourDao) : NavViewViewModel<AllToursViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        data class TourDetails(val pos: Int, val tour: ArticTour) : NavigationEndpoint()
    }

    val tours: Subject<List<AllToursCellViewModel>> = BehaviorSubject.create()
    val intro: Subject<String> = BehaviorSubject.createDefault("")

    init {
        toursDao.getTours()
                .map { list ->
                    val viewModelList = ArrayList<AllToursCellViewModel>()
                    list.forEach { tour ->
                        viewModelList.add(AllToursCellViewModel(tour))
                    }
                    return@map viewModelList
                }
                .bindTo(tours)
                .disposedBy(disposeBag)


        /**
         * Subscribe to locale change event.
         */
        Observables.combineLatest(
                languageSelector.appLanguageWithUpdates(disposeBag),
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
}

class AllToursCellViewModel(val tour: ArticTour) : CellViewModel() {
    val tourTitle: Subject<String> = BehaviorSubject.createDefault(tour.title)
    val tourDescription: Subject<String> = BehaviorSubject.createDefault(tour.description)
    val tourStops: Subject<String> = BehaviorSubject.createDefault("${tour.tourStops.count()}")
    val tourDuration: Subject<String> = BehaviorSubject.createDefault(tour.tourDuration)
    val tourImageUrl: Subject<String> = BehaviorSubject.createDefault(tour.standardImageUrl)
}