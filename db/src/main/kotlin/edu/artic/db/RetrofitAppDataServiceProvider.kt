package edu.artic.db

import com.fuzz.rx.bindTo
import edu.artic.db.progress.ProgressEventBus
import io.reactivex.Observable
import retrofit2.Retrofit
import javax.inject.Named

class RetrofitAppDataServiceProvider(@Named(ApiModule.RETROFIT_BLOB_API) retrofit: Retrofit, private val progressEventBus: ProgressEventBus) : AppDataServiceProvider {
    companion object {
        const val HEADER_ID = "blob_download_header_id"
    }

    private val service = retrofit.create(AppDataApi::class.java)

    override fun getBlob(): Observable<BlobState> {
        return Observable.create<BlobState> { observer ->
            progressEventBus.observable()
                    .subscribe {
                        if (it.downloadIdentifier == HEADER_ID) {
                            observer.onNext(BlobState.Downloading(it.progress / 100f))
                        }
                    }
            service.getBlob(HEADER_ID)
                    .map { BlobState.Done(it) }
                    .bindTo(observer)
        }
    }

    override fun getBlobHeaders(): Observable<Map<String, List<String>>> {
        return Observable.create { observer ->
            service.getBlobHeaders()
                    .subscribe({
                        observer.onNext(it.headers().toMultimap())
                    }, {
                        observer.onError(it)
                    }, {
                        observer.onComplete()
                    })

        }
    }
}