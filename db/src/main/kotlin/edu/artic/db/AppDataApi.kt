package edu.artic.db

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.db.models.ArticAppData
import edu.artic.db.models.ArticExhibition
import edu.artic.db.progress.Constants
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.*

interface AppDataApi {

    @HEAD("appData-v2.json")
    fun getBlobHeaders(): Observable<Response<Void>>


    @GET("appData-v2.json")
    fun getBlob(
            @Header(Constants.DOWNLOAD_IDENTIFIER_HEADER) header: String
    ): Observable<Result<ArticAppData>>

    @POST()
    fun getExhibitions(
            @Url url : String,
            @Body searchParams : MutableMap<String, Any>
    ) : Observable<Result<ArticResult<ArticExhibition>>>

}