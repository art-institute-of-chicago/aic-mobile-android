package artic.edu.search

import edu.artic.viewmodel.NavViewViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

open class SearchResultsBaseViewModel<NavEndpoint> @Inject constructor()
    : NavViewViewModel<NavEndpoint>() {

    val cells: Subject<List<SearchResultBaseCellViewModel>> = BehaviorSubject.create()

}