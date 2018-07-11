package edu.artic.db

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.db.models.ArticBlobData
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.HEAD

interface BlobApi {

    @HEAD("appData-v2.json")
    fun getBlobHeaders(): Observable<okhttp3.Response>

    @GET("appData-v2.json")
    fun getBlob(): Observable<Result<ArticBlobData>>
}