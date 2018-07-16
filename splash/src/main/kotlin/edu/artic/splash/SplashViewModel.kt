package edu.artic.splash

import android.util.Log
import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.viewmodel.BaseViewModel
import edu.artic.db.AppDataManager
import edu.artic.db.AppDataState
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.DashboardDao
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashViewModel @Inject constructor(appDataManager : AppDataManager) : NavViewViewModel<SplashViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class Welcome : NavigationEndpoint()
    }


    val percentage : PublishSubject<Float> = PublishSubject.create()

    init {
        appDataManager.getBlob()
                .subscribe({
                    when(it) {
                        is AppDataState.Downloading -> {
                            Log.d("SplashViewModel", "GetBlob: Downloading ${it.progress}")
                            percentage.onNext(it.progress)
                        }
                        is AppDataState.Done -> {
                            Log.d("SplashViewModel", "GetBlob: Done ${it.result.objects}")
                            Navigate.Forward(NavigationEndpoint.Welcome())
                                    .asObservable()
                                    .bindTo(navigateTo)
                                    .disposedBy(disposeBag)
                        }
                        is AppDataState.Empty -> {
                            Log.d("SplashViewModel", "GetBlob: Empty")
                        }
                    }
                },{
                    it.printStackTrace()
                },{})
                .disposedBy(disposeBag)
    }
}