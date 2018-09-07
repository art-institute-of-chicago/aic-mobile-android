package artic.edu.search

import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

open class SearchResultBaseCellViewModel(var hasDivider: Boolean = false) : BaseViewModel()

class SearchResultTextCellViewModel(text: String) : SearchResultBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(text)
}

class SearchResultEmptyCellViewModel : SearchResultBaseCellViewModel()

class SearchResultHeaderCellViewModel(text: String) : SearchResultBaseCellViewModel() {
    val text : Subject<String> = BehaviorSubject.createDefault(text)
}

class SearchResultArtworkCellViewModel : SearchResultBaseCellViewModel(hasDivider = true)

class SearchResultExhibitionCellViewModel : SearchResultBaseCellViewModel(hasDivider = true)

class SearchResultTourCellViewModel : SearchResultBaseCellViewModel(hasDivider = true)