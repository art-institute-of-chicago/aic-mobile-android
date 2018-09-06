package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchResultsSuggestedViewModel @Inject constructor(manager: SearchResultsManager) : BaseViewModel() {

    val cells: Subject<List<SearchResultBaseCellViewModel>> = BehaviorSubject.create()


    init {
        manager.currentSearchResults
                .map {
                    it.suggestions.map { SearchResultTextCellViewModel(it) }
                }.bindTo(cells)
                .disposedBy(disposeBag)

    }

}