package edu.artic.splash

import android.util.Log
import com.fuzz.rx.disposedBy
import edu.artic.viewmodel.BaseViewModel
import edu.artic.db.AppDataManager
import edu.artic.db.AppDataState
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.DashboardDao
import edu.artic.db.daos.GeneralInfoDao
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashViewModel @Inject constructor(appDataManager : AppDataManager) : BaseViewModel() {

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