package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import javax.inject.Inject

class SearchArtworkViewModel @Inject constructor(
        searchManager: SearchResultsManager
) : SearchBaseViewModel<SearchArtworkViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint

    init {
        searchManager.currentSearchResults
                .map { it.artworks }
                .map { list ->
                    if (list.isEmpty()) {
                        listOf(SearchResultEmptyCellViewModel())
                    } else {
                        list.map { SearchResultArtworkCellViewModel(it) }
                    }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)
    }
}