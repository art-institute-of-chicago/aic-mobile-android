package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchResultsExhibitionsViewModel @Inject constructor(
        searchManager: SearchResultsManager
) : SearchResultsBaseViewModel<SearchResultsExhibitionsViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint

    init {
        searchManager.currentSearchResults
                .map { it .exhibitions}
                .map { list ->
                    if(list.isEmpty()) {
                        listOf(SearchResultEmptyCellViewModel())
                    } else {
                        list.map { SearchResultExhibitionCellViewModel(it) }
                    }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)
    }
}