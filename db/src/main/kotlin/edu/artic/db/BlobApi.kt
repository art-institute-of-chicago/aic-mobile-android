package edu.artic.db

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.db.models.ArticBlobData
import edu.artic.db.progress.Constants
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header

interface BlobApi {

    @HEAD("appData-v2.json")
    fun getBlobHeaders(): Observable<Result<Response<Void>>>


    @GET("appData-v2.json")
    fun getBlob(@Header(Constants.DOWNLOAD_IDENTIFIER_HEADER) header: String): Observable<Result<ArticBlobData>>
}