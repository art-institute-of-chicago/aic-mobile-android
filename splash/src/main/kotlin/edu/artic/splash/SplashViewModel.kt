package edu.artic.splash

import android.util.Log
import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.AppDataManager
import edu.artic.db.ProgressDataState
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class SplashViewModel @Inject constructor(appDataManager: AppDataManager) : NavViewViewModel<SplashViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class Welcome : NavigationEndpoint()
    }


    val percentage: PublishSubject<Float> = PublishSubject.create()

    init {
        appDataManager.loadData()
                .subscribe({
                    when (it) {
                        is ProgressDataState.Downloading -> {
                            Log.d("SplashViewModel", "GetBlob: Downloading ${it.progress}")
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
                    it.printStackTrace()
                }, {}).disposedBy(disposeBag)
//        appDataManager.getBlob()
//                .subscribe({
//                    when(it) {
//                        is AppDataState.Downloading -> {
//                            Log.d("SplashViewModel", "GetBlob: Downloading ${it.progress}")
//                            percentage.onNext(it.progress)
//                        }
//                        is AppDataState.Done -> {
//                            goToWelcome()
//                        }
//                        is AppDataState.Empty -> {
//                            goToWelcome()
//                        }
//                    }
//                },{
//                    it.printStackTrace()
//                },{})
//                .disposedBy(disposeBag)
    }

    private fun goToWelcome() {
        Navigate.Forward(NavigationEndpoint.Welcome())
                .asObservable()
                .bindTo(navigateTo)
                .disposedBy(disposeBag)
    }
}