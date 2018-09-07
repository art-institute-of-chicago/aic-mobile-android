package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import javax.inject.Inject

class SearchResultsArtworkViewModel @Inject constructor(
        searchManager: SearchResultsManager
) : SearchResultsBaseViewModel() {

    init {
        searchManager.currentSearchResults
                .map { result ->
                    result.artworks.map { SearchResultArtworkCellViewModel(it) }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)
    }
}