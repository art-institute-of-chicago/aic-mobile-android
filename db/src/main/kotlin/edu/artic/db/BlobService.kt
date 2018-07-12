package edu.artic.db

import android.util.Log
import io.reactivex.Observable
import javax.inject.Inject

class BlobService @Inject constructor(val provider: BlobProvider) {

    fun getBlob() : Observable<BlobState> {
        return provider.getBlobHeaders()
                .flatMap {
                    if(it.containsKey("Last-Modified") ) {
                        Log.d("BlobService", "lastModified: " + it["Last-Modified"])
                        //Add check for distance from last download
//                        return@flatMap Observable.just(BlobState.Empty())
                    }
                    provider.getBlob()

                }.doOnNext {
                    if(it is BlobState.Done) {
                        //Save to db
                    }
                }
    }
}