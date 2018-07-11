package edu.artic.db

import io.reactivex.Observable
import retrofit2.http.GET

interface BlobProvider {

    fun getBlobHeaders() : Observable<Map<String, List<String>>>
    fun getBlob(): Observable<BlobState>
}