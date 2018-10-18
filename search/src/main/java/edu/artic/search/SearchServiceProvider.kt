package edu.artic.search

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.db.models.ApiSearchResult
import io.reactivex.Observable


/**
 * Contract between a [SearchViewModel] and a [retrofit2.Retrofit]. The
 * Retrofit contract is defined separately, in [SearchApi].
 *
 * Easily mocked for tests and so forth.
 *
 * @see edu.artic.db.AppDataServiceProvider
 */
interface SearchServiceProvider {

    /**
     * @see RetrofitSearchServiceProvider.getSuggestions
     */
    fun getSuggestions(searchQuery: String): Observable<Result<List<String>>>

    fun loadAllMatchingContent(searchQuery: String): Observable<ApiSearchResult>

}

/**
 * One of the two bundled implementations of [SearchServiceProvider],
 * along with [RetrofitSearchServiceProvider].
 *
 * This will always error out.
 */
object NoSearchResultsServiceProvider: SearchServiceProvider {
    override fun getSuggestions(searchQuery: String): Observable<Result<List<String>>> {
        return Observable.error(
                SearchUnavailableError
        )
    }

    override fun loadAllMatchingContent(searchQuery: String): Observable<ApiSearchResult> {
        return Observable.error(
                SearchUnavailableError
        )
    }
}
