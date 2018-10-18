package edu.artic.search

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.db.models.*
import io.reactivex.Observable
import retrofit2.http.*


/**
 * Official API endpoints for looking up search suggestions and search results.
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
interface SearchApi {


    /**
     * Callers should generally not need to override the default value of [resources].
     *
     * You'll need to derive [autoCompleteUrl] from
     * [edu.artic.db.models.ArticDataObject.autocompleteEndpoint] at runtime.
     */
    @GET()
    fun getSuggestions(
            @Url autoCompleteUrl: String,
            @Query("q") searchQuery: String,
            @Query("resources") resources: String = "artworks,tours,exhibitions,artists"
    ): Observable<Result<List<String>>>

    @POST()
    fun loadMatchingArtworkContent(
            @Url multiSearchUrl: String,
            @Body queryContent: MutableList<MutableMap<String, Any>>
    ): Observable<Result<List<ApiSearchResultRawA>>>

    @POST()
    fun loadMatchingTourContent(
            @Url multiSearchUrl: String,
            @Body queryContent: MutableList<MutableMap<String, Any>>
    ): Observable<Result<List<ApiSearchResultRawT>>>

    @POST()
    fun loadMatchingExhibitionContent(
            @Url multiSearchUrl: String,
            @Body queryContent: MutableList<MutableMap<String, Any>>
    ): Observable<Result<List<ApiSearchResultRawE>>>
}