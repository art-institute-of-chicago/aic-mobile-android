package artic.edu.search

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url


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
}