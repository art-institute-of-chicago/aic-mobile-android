package artic.edu.search

import com.fuzz.rx.bindTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * Handles loading and storage of search results. As well as an abstract way of keeping state of
 * current view visible
 * @author Piotr Leja (FUZZ)
 */
class SearchResultsManager(private val searchService: SearchServiceProvider) {

    val currentSearchResults: Subject<ArticSearchResult> = BehaviorSubject.create()
    val currentSearchText: Subject<String> = BehaviorSubject.create()

    init {
        setupTextAvailableSearchFlow()
        setupEmptyTextSearchFlow()
    }

    private fun setupTextAvailableSearchFlow() {
        currentSearchText
                .filter { it.isNotEmpty() }
                .flatMap { searchService.getSuggestions(it) }
                .map {
                    if (it.response().body() == null) {
                        emptyList()
                    } else {
                        it.response().body()
                    }
                }
                .map {
                    ArticSearchResult(
                            it,
                            emptyList(),
                            emptyList(),
                            emptyList()
                    )
                }.bindTo(currentSearchResults)
    }

    private fun setupEmptyTextSearchFlow() {
        currentSearchText
                .filter { it.isEmpty() }
                .map {
                    ArticSearchResult(
                            emptyList(),
                            emptyList(),
                            emptyList(),
                            emptyList()
                    )
                }
                .bindTo(currentSearchResults)
    }

    fun onChangeSearchText(newText: String) {
        currentSearchText.onNext(newText)
    }

}