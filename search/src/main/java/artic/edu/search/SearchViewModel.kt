package artic.edu.search

import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import javax.inject.Inject

class SearchViewModel @Inject constructor(private val analyticsTracker: AnalyticsTracker,
                                          private val searchResultsManager: SearchResultsManager)
    : NavViewViewModel<SearchViewModel.NavigationEndpoint>() {

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
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.DefaultSearchResults))
        } else {
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.DynamicSearchResults))
        }
    }

}

