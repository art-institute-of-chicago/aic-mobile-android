package edu.artic.splash

import android.util.Log
import com.fuzz.rx.disposedBy
import edu.artic.viewmodel.BaseViewModel
import edu.artic.db.AppDataManager
import edu.artic.db.AppDataState
import javax.inject.Inject

class SplashViewModel @Inject constructor(appDataManager : AppDataManager) : BaseViewModel() {

    init {
        appDataManager.getBlob()
                .subscribe({
                    when(it) {
                        is AppDataState.Downloading -> {
                            Log.d("SplashViewModel", "GetBlob: Downloading ${it.progress}")
                        }
                        is AppDataState.Done -> {
                            Log.d("SplashViewModel", "GetBlob: Done ${it.result.objects}")
                        }
                        is AppDataState.Empty -> {
                            Log.d("SplashViewModel", "GetBlob: Empty")
                        }
                    }
                },{},{})
                .disposedBy(disposeBag)
    }

}