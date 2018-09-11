package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import javax.inject.Inject

class SearchToursViewModel @Inject constructor(
        searchManager: SearchResultsManager,
        analyticsTracker: AnalyticsTracker
) : SearchBaseViewModel(analyticsTracker, searchManager)  {

    sealed class NavigationEndpoint

    init {
        searchManager.currentSearchResults
                .map { it.tours }
                .map { result ->
                    if (result.isEmpty()) {
                        listOf(SearchEmptyCellViewModel())
                    } else {
                        result.map { SearchTourCellViewModel(it) }
                    }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)
    }
}