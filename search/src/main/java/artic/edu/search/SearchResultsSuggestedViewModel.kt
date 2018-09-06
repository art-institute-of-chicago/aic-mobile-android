package artic.edu.search

import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchResultsSuggestedViewModel @Inject constructor(val manager: SearchResultsManager) : BaseViewModel() {

    val cells: Subject<List<SearchResultBaseCellViewModel>> = BehaviorSubject.createDefault(
            mutableListOf(
                    SearchResultTextCellViewModel("test"),
                    SearchResultTextCellViewModel("test1"),
                    SearchResultTextCellViewModel("test2"),
                    SearchResultTextCellViewModel("test3")
            )
    )




}