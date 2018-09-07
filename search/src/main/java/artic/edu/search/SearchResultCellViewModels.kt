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
    val text: Subject<String> = BehaviorSubject.createDefault(text)
}

open class SearchResultBaseListItemViewModel(isHeadphonesVisisble: Boolean = false)
    : SearchResultBaseCellViewModel(hasDivider = true) {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val itemTitle: Subject<String> = BehaviorSubject.create()
    val itemSubTitle: Subject<String> = BehaviorSubject.create()
    val isHeadphonesVisisble: Subject<Boolean> = BehaviorSubject.createDefault(isHeadphonesVisisble)
}

class SearchResultArtworkCellViewModel
    : SearchResultBaseListItemViewModel(isHeadphonesVisisble = true)

class SearchResultExhibitionCellViewModel : SearchResultBaseListItemViewModel()

class SearchResultTourCellViewModel : SearchResultBaseListItemViewModel()