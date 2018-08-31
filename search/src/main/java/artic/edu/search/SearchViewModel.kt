package artic.edu.search

import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModel
import javax.inject.Inject

class SearchViewModel @Inject constructor(private val analyticsTracker: AnalyticsTracker) : BaseViewModel() {

    fun clearText() {
        analyticsTracker.reportEvent(ScreenCategoryName.Search, AnalyticsAction.searchAbandoned)
    }

}

