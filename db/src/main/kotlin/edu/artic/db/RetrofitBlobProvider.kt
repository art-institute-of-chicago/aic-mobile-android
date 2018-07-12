package edu.artic.db

import android.util.Log
import edu.artic.db.progress.ProgressEventBus
import io.reactivex.Observable
import retrofit2.Retrofit
import javax.inject.Named

class RetrofitBlobProvider(@Named(ApiModule.RETROFIT_BLOB_API) retrofit: Retrofit, private val progressEventBus: ProgressEventBus) : BlobProvider {
    companion object {
        const val HEADER_ID = "blob_download_header_id"
    }

    private val service = retrofit.create(BlobApi::class.java)

    override fun getBlob(): Observable<BlobState> {
        return Observable.create<BlobState> { observer ->
            //TODO: Create subsribe to some kind of progress listener and then send that to observer in BlobState.Downloading()
            progressEventBus.observable()
                    .subscribe {
                        if (it.downloadIdentifier == HEADER_ID) {
                            observer.onNext(BlobState.Downloading(it.progress / 100f))
                        }
                    }
            service.getBlob(HEADER_ID)
                    .subscribe({
                        observer.onNext(BlobState.Done(it))
                    }, {
                        observer.onError(it)
                    }, {
                        observer.onComplete()
                    })
        }
    }

    override fun getBlobHeaders(): Observable<Map<String, List<String>>> {
        return Observable.create { observer ->
            service.getBlobHeaders()
                    .subscribe{
                        Log.d("BlobProvider", "isError ${it.isError}")
//                        observer.onNext(it.headers().toMultimap())
                    }
//            , {
//                        observer.onError(it)
//                    }, {
//                        observer.onComplete()
//                    })

        }
    }
}