package artic.edu.search

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * Handles loading and storage of search results. As well as an abstract way of keeping state of
 * current view visible
 * @author Piotr Leja (FUZZ)
 */
class SearchResultsManager {

    val currentSearchResults : Subject<ArticSearchResult> = BehaviorSubject.create()

    init {
        val result = ArticSearchResult(
                listOf("test", "test2", "test3"),
                emptyList(),
                emptyList(),
                emptyList()
        )
        currentSearchResults.onNext(result)
    }

}