package artic.edu.search

import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

open class SearchResultsBaseViewModel @Inject constructor()
    : BaseViewModel() {

    val cells: Subject<List<SearchResultBaseCellViewModel>> = BehaviorSubject.create()

}