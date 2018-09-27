package edu.artic.splash

import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.AppDataManager
import edu.artic.db.ProgressDataState
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashViewModel @Inject constructor(appDataManager: AppDataManager,
                                          analyticsTracker: AnalyticsTracker) :
        NavViewViewModel<SplashViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class Welcome : NavigationEndpoint()
        class Language: NavigationEndpoint()
        class Loading: NavigationEndpoint()
    }


    val percentage: PublishSubject<Float> = PublishSubject.create()

    init {
        analyticsTracker.clearSession()
        appDataManager.loadData()
                .subscribe({
                    when (it) {
                        is ProgressDataState.Interrupted -> {
                            percentage.onError(it.error)
                        }
                        is ProgressDataState.Downloading -> {
                            percentage.onNext(it.progress)
                        }
                        is ProgressDataState.Done<*> -> {
                            goToWelcome()
                        }
                        is ProgressDataState.Empty -> {
                            goToWelcome()
                        }
                    }
                }, {
                    percentage.onError(it)
                }, {}).disposedBy(disposeBag)
    }

    private fun goToWelcome() {
        Navigate.Forward(NavigationEndpoint.Loading())
                .asObservable().delay(1, TimeUnit.SECONDS)
                .bindTo(navigateTo)
                .disposedBy(disposeBag)
    }
}