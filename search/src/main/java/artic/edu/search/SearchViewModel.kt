package artic.edu.search

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

    val closeButtonVisible : Subject<Boolean> = BehaviorSubject.createDefault(false)
    val shouldClearTextInput : Subject<Boolean> = BehaviorSubject.createDefault(false)

    sealed class NavigationEndpoint {
        object DefaultSearchResults : NavigationEndpoint()
        object DynamicSearchResults : NavigationEndpoint()
    }

    fun clearText() {
        analyticsTracker.reportEvent(ScreenCategoryName.Search, AnalyticsAction.searchAbandoned)
    }

    fun onTextChanged(newText: String) {
        //TODO pass new text to manager
        if (newText.isEmpty()) {
            closeButtonVisible.onNext(false)
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.DefaultSearchResults))
        } else {
            closeButtonVisible.onNext(true)
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.DynamicSearchResults))
        }
    }

    fun onCloseClicked() {
        shouldClearTextInput.onNext(true)
        shouldClearTextInput.onNext(false)
        onTextChanged("")
    }

}

