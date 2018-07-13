package edu.artic.splash

import android.util.Log
import com.fuzz.rx.disposedBy
import edu.artic.viewmodel.BaseViewModel
import edu.artic.db.AppDataManager
import edu.artic.db.AppDataState
import edu.artic.db.daos.DashboardDao
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashViewModel @Inject constructor(appDataManager : AppDataManager, val dashboardDao: DashboardDao) : BaseViewModel() {

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
                            getDashboard()
                        }
                        is AppDataState.Empty -> {
                            Log.d("SplashViewModel", "GetBlob: Empty")
                        }
                    }
                },{},{})
                .disposedBy(disposeBag)
    }

    fun getDashboard() {
        Observable.just(true).delay(10, TimeUnit.SECONDS).subscribe {
            val dashboard = dashboardDao.getCurrentDashboad()
            Log.d("SplashViewModel: ","dashoard  $dashboard")
        }.disposedBy(disposeBag)
    }

}