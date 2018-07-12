package edu.artic.db

import io.reactivex.Observable
import retrofit2.Retrofit
import javax.inject.Named

class RetrofitBlobProvider(@Named(ApiModule.RETROFIT_BLOB_API) retrofit: Retrofit) : BlobProvider {

    private val service = retrofit.create(BlobApi::class.java)

    override fun getBlob(): Observable<BlobState> {
        return Observable.create<BlobState> { observer ->

            //TODO: Create subsribe to some kind of progress listener and then send that to observer in BlobState.Downloading()

            service.getBlob().subscribe({
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
            service.getBlobHeaders().subscribe({
                observer.onNext(it.headers().toMultimap())
            }, {
                observer.onError(it)
            }, {
                observer.onComplete()
            })

        }
    }
}