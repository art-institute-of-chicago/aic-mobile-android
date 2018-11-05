package edu.artic.db

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import com.jobinlawrance.downloadprogressinterceptor.DOWNLOAD_IDENTIFIER_HEADER
import edu.artic.db.models.ArticAppData
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.HEAD
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import retrofit2.http.Body

interface AppDataApi {

    @HEAD("appData-v2.json")
    fun getBlobHeaders(): Observable<Response<Void>>


    @GET("appData-v2.json")
    fun getBlob(
            @Header(DOWNLOAD_IDENTIFIER_HEADER) header: String
    ): Observable<Result<ArticAppData>>

    @POST()
    fun getExhibitions(
            @Header(DOWNLOAD_IDENTIFIER_HEADER) header: String,
            @Url url : String,
            @Body searchParams : MutableMap<String, Any>
    ) : Observable<Result<ArticResult<ArticExhibition>>>

    @POST()
    fun getEvents(
            @Header(DOWNLOAD_IDENTIFIER_HEADER) header: String,
            @Url url : String,
            @Body searchParams : MutableMap<String, Any>
    ) : Observable<Result<ArticResult<ArticEvent>>>
}