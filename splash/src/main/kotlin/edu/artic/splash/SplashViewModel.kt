package edu.artic.splash

import com.fuzz.rx.disposedBy
import edu.artic.viewmodel.BaseViewModel
import edu.artic.db.BlobService
import javax.inject.Inject

class SplashViewModel @Inject constructor(private val blobService : BlobService) : BaseViewModel() {

    init {
        blobService.getBlob()
                .subscribe({},{},{})
                .disposedBy(disposeBag)
    }

}