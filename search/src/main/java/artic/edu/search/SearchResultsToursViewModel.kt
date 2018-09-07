package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import javax.inject.Inject

class SearchResultsToursViewModel @Inject constructor(
        searchManager: SearchResultsManager
) : SearchResultsBaseViewModel() {

    init {
        searchManager.currentSearchResults
                .map{it.tours}
                .map { result ->
                    if(result.isEmpty()) {
                        listOf(SearchResultEmptyCellViewModel())
                    } else{
                        result.map { SearchResultTourCellViewModel(it) }
                    }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)
    }
}