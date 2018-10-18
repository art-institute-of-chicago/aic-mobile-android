package edu.artic.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.ArticGalleryDao
import javax.inject.Inject

class SearchToursViewModel @Inject constructor(
        searchManager: SearchResultsManager,
        analyticsTracker: AnalyticsTracker,
        dataObjectDao: ArticDataObjectDao,
        galleryDao: ArticGalleryDao
) : SearchBaseViewModel(analyticsTracker, searchManager, dataObjectDao, galleryDao)  {

    sealed class NavigationEndpoint

    init {
        searchManager.currentSearchResults
                .filter { !it.searchTerm.isBlank() }
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