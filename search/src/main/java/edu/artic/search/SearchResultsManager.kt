package edu.artic.search

import com.fuzz.rx.bindTo
import edu.artic.db.INVALID_FLOOR
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.models.*
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

/**
 * Handles loading and storage of search results. As well as an abstract way of keeping state of
 * current view visible
 * @author Piotr Leja (FUZZ)
 */
class SearchResultsManager(private val searchService: SearchServiceProvider,
                           private val tourDao: ArticTourDao,
                           private val articObjectDao: ArticObjectDao,
                           private val articDataObjectDao: ArticDataObjectDao,
                           private val articGalleryDao: ArticGalleryDao
) {


    private val rawSearchResults: Subject<ArticSearchResult> = BehaviorSubject.create()
    val currentSearchResults: Subject<ArticSearchResult> = BehaviorSubject.create()
    val currentSearchText: Subject<String> = BehaviorSubject.create()
    private val showSuggestions: Subject<Boolean> = BehaviorSubject.create()

    init {
        setupTextAvailableSearchFlow()
        setupEmptyTextSearchFlow()
        Observables.combineLatest(showSuggestions.distinctUntilChanged(), rawSearchResults)
        { showSuggestions, results ->
            if (!showSuggestions) {
                results.suggestions = emptyList()
            }
            results
        }.bindTo(currentSearchResults)
    }

    private fun setupTextAvailableSearchFlow() {
        currentSearchText
                .throttleWithTimeout(250, TimeUnit.MILLISECONDS)
                .filter { it.isNotEmpty() }
                .flatMap { searchTerm ->
                    Observables.combineLatest(
                            getSuggestionsList(searchTerm),
                            loadOtherLists(searchTerm)
                    )
                }
                .map { (suggestions, searchResult) ->
                    searchResult.suggestions = suggestions
                    return@map searchResult

                }.bindTo(rawSearchResults)
    }

    private fun setupEmptyTextSearchFlow() {
        currentSearchText
                .throttleWithTimeout(250, TimeUnit.MILLISECONDS)
                .filter { it.isEmpty() }
                .map {
                    ArticSearchResult(
                            "",
                            emptyList(),
                            emptyList(),
                            emptyList(),
                            emptyList()
                    )
                }
                .bindTo(currentSearchResults)
    }

    private fun getSuggestionsList(searchTerm: String): Observable<List<String>> {
        return searchService.getSuggestions(searchTerm)
                .map {
                    if (it.response().body() == null) {
                        emptyList()
                    } else {
                        it.response().body()
                    }
                }
    }

    private fun loadOtherLists(searchTerm: String): Observable<ArticSearchResult> {
        return searchService
                .loadAllMatchingContent(searchTerm)
                .flatMap { result ->
                    Observables.combineLatest(
                            loadArtwork(result.artworks),
                            loadTours(result.tours),
                            if (result.exhibitions == null)
                                Observable.just(listOf())
                            else
                                Observable.just(result.exhibitions!!)
                    ) { artwork: List<ArticSearchArtworkObject>, tours: List<ArticTour>, exhibitions: List<ArticExhibition> ->
                        ArticSearchResult(searchTerm, emptyList(), artwork, tours, exhibitions)
                    }
                }
    }

    private fun loadTours(tours: List<ApiSearchContent.SearchedTour>?): Observable<List<ArticTour>> {
        return if (tours == null) {
            Observable.just(emptyList())
        } else {
            tourDao.getToursByIdList(tours.map { it.tourId.toString() }).toObservable()
        }
    }

    private fun loadArtwork(artwork: List<ApiSearchContent.SearchedArtwork>?): Observable<List<ArticSearchArtworkObject>> {
        return articDataObjectDao.getDataObject()
                //Get the base Server url just in case we need it... which we might
                .map { dataObject -> dataObject.imageServerUrl }
                .toObservable()
                .flatMap {
                    generateArtworksObjectList(it, artwork)
                }

    }

    private fun generateArtworksObjectList(baseUrl: String,
                                          artwork: List<ApiSearchContent.SearchedArtwork>?)
            : Observable<List<ArticSearchArtworkObject>> {

        //Create new observable to handle merging the list and stuff
        return Observable.create<List<ArticSearchArtworkObject>> { emitter ->
            val returnList = mutableListOf<ArticSearchArtworkObject>()
            artwork?.forEach { searchedArtwork ->
                val artworkId = searchedArtwork.artworkId.toString()
                val articObject = articObjectDao.getObjectByIdSynchronously(artworkId)
                val gallery = articGalleryDao.getGalleryForIdSynchronously(searchedArtwork.gallery_id)
                if (articObject != null) {
                    returnList.add(
                            transformArticObjectToArticSearchObject(
                                    artworkId,
                                    articObject,
                                    gallery
                            )
                    )
                } else if (searchedArtwork.isOnView) {
                    returnList.add(
                            transformSearchedArtworkToSearchArtworkObject(
                                    searchedArtwork,
                                    baseUrl,
                                    gallery
                            )
                    )
                }

            }

            emitter.onNext(returnList)
            emitter.onComplete()
        }
    }

    private fun transformSearchedArtworkToSearchArtworkObject(
            searchedArtwork: ApiSearchContent.SearchedArtwork,
            baseUrl: String,
            gallery: ArticGallery?
    ): ArticSearchArtworkObject {

        val imageBaseUrl = baseUrl + "/" + searchedArtwork.image_id

        val thumbnailUrl = "$imageBaseUrl/full/!200,200/0/default.jpg"
        val imageUrl = "$imageBaseUrl/full/!800,800/0/default.jpg"

        return ArticSearchArtworkObject(
                searchedArtwork.artworkId.toString(),
                null,
                searchedArtwork.title,
                thumbnailUrl,
                imageUrl,
                searchedArtwork.artist_display,
                searchedArtwork.latlon,
                gallery?.floor ?: INVALID_FLOOR,
                gallery
        )
    }

    private fun transformArticObjectToArticSearchObject(artworkId: String,
                                                        articObject: ArticObject,
                                                        gallery: ArticGallery?)
            : ArticSearchArtworkObject {

        return ArticSearchArtworkObject(
                artworkId,
                articObject,
                articObject.title,
                articObject.thumbUrl,
                articObject.image_url,
                articObject.tombstone ?: "",
                articObject.location,
                gallery?.floor ?: INVALID_FLOOR,
                gallery
        )
    }

    fun onChangeSearchText(newText: String) {
        showSuggestions.onNext(true)
        currentSearchText.onNext(newText)
    }

    fun search(newText: String? = null) {
        showSuggestions.onNext(false)
        if (newText != null) {
            currentSearchText.onNext(newText)
        }
    }


}