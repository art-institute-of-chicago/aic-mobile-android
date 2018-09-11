package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import javax.inject.Inject

class SearchExhibitionsViewModel @Inject constructor(
        searchManager: SearchResultsManager
) : SearchBaseViewModel<SearchExhibitionsViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint

    init {
        searchManager.currentSearchResults
                .map { it.exhibitions }
                .map { list ->
                    if (list.isEmpty()) {
                        listOf(SearchEmptyCellViewModel())
                    } else {
                        list.map { SearchExhibitionCellViewModel(it) }
                    }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)
    }
}