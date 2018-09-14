package edu.artic.search

import com.fuzz.rx.bindTo
import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.db.ApiBodyGenerator
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.models.ApiSearchResult
import edu.artic.db.models.ArticDataObject
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
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

    /**
     * This method is intended to provide consistent, abstracted data to
     * [SearchResultsManager]. At this time, network errors are folded into
     * empty lists.
     *
     * In the swift codebase the equivalent method is called 'loadAllContent'.
     *
     * NB: As usual, the caller is responsible for disposing of the returned
     * observable in a timely manner.
     */
    override fun loadAllMatchingContent(searchQuery: String): Observable<ApiSearchResult> {

        val queryContentA = ApiBodyGenerator.createSearchArtworkQueryBody(searchQuery)
        val queryContentT = ApiBodyGenerator.createSearchTourQueryBody(searchQuery)
        val queryContentE = ApiBodyGenerator.createSearchExhibitionQueryBody(searchQuery)

        return dataObject.take(1)
                .observeOn(Schedulers.io())
                .switchMap { config ->
                    Observables.zip(
                            api.loadMatchingArtworkContent(
                                    multiSearchUrl = config.dataApiUrl + config.multiSearchEndpoint,
                                    queryContent = mutableListOf(queryContentA)
                            ).mapWithDefault(emptyList()),
                            api.loadMatchingTourContent(
                                    multiSearchUrl = config.dataApiUrl + config.multiSearchEndpoint,
                                    queryContent = mutableListOf(queryContentT)
                            ).mapWithDefault(emptyList()),
                            api.loadMatchingExhibitionContent(
                                    multiSearchUrl = config.dataApiUrl + config.multiSearchEndpoint,
                                    queryContent = mutableListOf(queryContentE)
                            ).mapWithDefault(emptyList())
                    )
                }.map {
                    (a, t, e) ->

                    ApiSearchResult(
                            a.firstOrNull()?.internalData.orEmpty(),
                            t.firstOrNull()?.internalData.orEmpty(),
                            e.firstOrNull()?.internalData.orEmpty()
                    )
                }
    }
}