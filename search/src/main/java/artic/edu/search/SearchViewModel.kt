package artic.edu.search

import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchViewModel @Inject constructor(private val analyticsTracker: AnalyticsTracker,
                                          private val searchResultsManager: SearchResultsManager)
    : NavViewViewModel<SearchViewModel.NavigationEndpoint>() {

    val searchText : Subject<String> = BehaviorSubject.create()
    val closeButtonVisible : Subject<Boolean> = BehaviorSubject.createDefault(false)
    val shouldClearTextInput : Subject<Boolean> = BehaviorSubject.createDefault(false)
    sealed class NavigationEndpoint {
        object DefaultSearchResults : NavigationEndpoint()
        object DynamicSearchResults : NavigationEndpoint()
    }

    init {
        searchResultsManager.currentSearchText
                .distinctUntilChanged()
                .bindToMain(searchText)
                .disposedBy(disposeBag)
    }

    fun clearText() {
        analyticsTracker.reportEvent(ScreenCategoryName.Search, AnalyticsAction.searchAbandoned)
    }

    fun onTextChanged(newText: String) {
        searchResultsManager.onChangeSearchText(newText)
        if (newText.isEmpty()) {
            closeButtonVisible.onNext(false)
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.DefaultSearchResults))
        } else {
            closeButtonVisible.onNext(true)
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.DynamicSearchResults))
        }
    }

    fun onClickSearch() {
        searchResultsManager.search()
    }

    fun onCloseClicked() {
        clearText()
        shouldClearTextInput.onNext(true)
        shouldClearTextInput.onNext(false)
        onTextChanged("")
    }

}

