package edu.artic.db

import io.reactivex.Observable
import javax.inject.Inject

class BlobService @Inject constructor(val provider: BlobProvider) {

    fun getBlob() : Observable<BlobState> {
        return provider.getBlobHeaders()
                .flatMap {
                    if(it.containsKey("Last-Modified") ) {
                        //Add check for distance from last download
                        return@flatMap Observable.just(BlobState.Empty())
                    } else {
                        provider.getBlob()
                    }

                }.doOnNext {
                    if(it is BlobState.Done) {
                        //Save to db
                    }
                }
    }
}