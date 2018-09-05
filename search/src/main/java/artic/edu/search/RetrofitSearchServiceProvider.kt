package artic.edu.search

import com.fuzz.rx.bindTo
import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.models.ArticDataObject
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import retrofit2.Retrofit

/**
 * [Retrofit]-backed implementation of [SearchServiceProvider].
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
class RetrofitSearchServiceProvider(
        retrofit: Retrofit,
        dataObjectDao: ArticDataObjectDao
) : SearchServiceProvider {


    private val dataObject: Subject<ArticDataObject> = BehaviorSubject.create()

    private val api: SearchApi = retrofit.create(SearchApi::class.java)

    init {
        dataObjectDao
                .getDataObject()
                .bindTo(dataObject)
    }


    /**
     * Base url for query:
     *
     * * [ArticDataObject.dataApiUrl] + [ArticDataObject.autocompleteEndpoint]
     *
     * Constant query parameter (defined directly on [SearchApi.getSuggestions]):
     *
     * * resources="artworks,tours,exhibitions,artists"
     */
    override fun getSuggestions(searchQuery: String): Observable<Result<List<String>>> {
        return dataObject.take(1)
                .observeOn(Schedulers.io())
                .switchMap { config ->
                    api.getSuggestions(
                            autoCompleteUrl = config.dataApiUrl + config.autocompleteEndpoint,
                            searchQuery = searchQuery
                    )
                }

    }
}