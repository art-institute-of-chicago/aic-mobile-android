package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import javax.inject.Inject

class SearchResultsToursViewModel @Inject constructor(
        searchManager: SearchResultsManager
) : SearchResultsBaseViewModel() {

    init {
        searchManager.currentSearchResults
                .map { result ->
                    //TODO; add empty error message
                    result.tours.map { SearchResultTourCellViewModel(it) }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)
    }
}