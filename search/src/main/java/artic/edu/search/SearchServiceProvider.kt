package artic.edu.search

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
