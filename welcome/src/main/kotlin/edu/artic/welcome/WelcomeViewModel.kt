package edu.artic.welcome

import android.arch.lifecycle.LifecycleOwner
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class WelcomeViewModel @Inject constructor(private val welcomePreferencesManager: WelcomePreferencesManager) : BaseViewModel() {

    val shouldPeekTourSummary: Subject<Boolean> = BehaviorSubject.create()
    val tours: Subject<List<TourCellViewModel>> = BehaviorSubject.create()
    val exhibitions: Subject<List<OnViewViewModel>> = BehaviorSubject.create()

    init {
        shouldPeekTourSummary.distinctUntilChanged()
                .subscribe {
                    welcomePreferencesManager.shouldPeekTourSummary = it
                }.disposedBy(disposeBag)
    }

    override fun register(lifeCycleOwner: LifecycleOwner) {
        super.register(lifeCycleOwner)
        shouldPeekTourSummary.onNext(welcomePreferencesManager.shouldPeekTourSummary)
    }


    fun onPeekedTour() {
        Observable.just(false)
                .bindTo(this.shouldPeekTourSummary)
                .disposedBy(disposeBag)
    }

    /**
     * Temp method until dao is ready
     */
    fun addTours(tours: List<ArticTour>) {
        val viewModelList = ArrayList<TourCellViewModel>()
        tours.forEach {
            viewModelList.add(TourCellViewModel(it))
        }
        this.tours.onNext(viewModelList)
    }

    /**
     * Temp method to until dao is ready
     */
    fun addExhibitions(exhibitions: List<ArticExhibition>) {
        val viewModelList = ArrayList<OnViewViewModel>()
        exhibitions.forEach {
            viewModelList.add(OnViewViewModel(it))
        }
        this.exhibitions.onNext(viewModelList)
    }
}