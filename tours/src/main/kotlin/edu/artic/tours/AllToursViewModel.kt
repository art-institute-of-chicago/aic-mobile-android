package edu.artic.tours

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class AllToursViewModel @Inject constructor(toursDao : ArticTourDao)  : BaseViewModel() {

    val tours : Subject<List<AllToursCellViewModel>> = BehaviorSubject.create()

    init {
        toursDao.getTours()
                .map { list ->
                    val viewModelList = ArrayList<AllToursCellViewModel>()
                    list.forEach{ tour ->
                        viewModelList.add(AllToursCellViewModel(tour))
                    }
                    return@map viewModelList
                }
                .bindTo(tours)
                .disposedBy(disposeBag)
    }

}

class AllToursCellViewModel(tour : ArticTour) : BaseViewModel() {
    val tourTitle: Subject<String> = BehaviorSubject.createDefault(tour.title)
    val tourDescription: Subject<String> = BehaviorSubject.createDefault(tour.description)
    val tourStops: Subject<String> = BehaviorSubject.createDefault("${tour.tourStops.count() ?: 0}")
    val tourDuration: Subject<String> = BehaviorSubject.createDefault(tour.tourDuration)
    val tourImageUrl: Subject<String> = BehaviorSubject.createDefault(tour.imageUrl)
}