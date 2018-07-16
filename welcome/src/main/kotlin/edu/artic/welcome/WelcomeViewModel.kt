package edu.artic.welcome

import android.arch.lifecycle.LifecycleOwner
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
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
}