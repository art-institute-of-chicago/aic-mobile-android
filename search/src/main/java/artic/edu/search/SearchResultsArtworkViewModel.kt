package artic.edu.search

import com.fuzz.rx.disposedBy
import javax.inject.Inject

class SearchResultsArtworkViewModel @Inject constructor(
        searchManager: SearchResultsManager
) : SearchResultsBaseViewModel() {

    init {
        searchManager.currentSearchResults
                .map {
                    it.artworks
                }
                .subscribe { }
                .disposedBy(disposeBag)
    }
}