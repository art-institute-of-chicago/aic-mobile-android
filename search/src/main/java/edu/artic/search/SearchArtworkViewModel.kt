package edu.artic.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import javax.inject.Inject

class SearchArtworkViewModel @Inject constructor(
        searchManager: SearchResultsManager,
        analyticsTracker: AnalyticsTracker
) : SearchBaseViewModel(analyticsTracker, searchManager) {

    sealed class NavigationEndpoint

    init {
        searchManager.currentSearchResults
                .filter { !it.searchTerm.isBlank() }
                .map { it.artworks }
                .map { list ->
                    if (list.isEmpty()) {
                        listOf(SearchEmptyCellViewModel())
                    } else {
                        list.map { SearchArtworkCellViewModel(it) }
                    }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)
    }
}