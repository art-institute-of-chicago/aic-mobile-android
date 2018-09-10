package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import javax.inject.Inject

class SearchToursViewModel @Inject constructor(
        searchManager: SearchResultsManager
) : SearchBaseViewModel<SearchToursViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint

    init {
        searchManager.currentSearchResults
                .map { it.tours }
                .map { result ->
                    if (result.isEmpty()) {
                        listOf(SearchResultEmptyCellViewModel())
                    } else {
                        result.map { SearchResultTourCellViewModel(it) }
                    }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)
    }
}