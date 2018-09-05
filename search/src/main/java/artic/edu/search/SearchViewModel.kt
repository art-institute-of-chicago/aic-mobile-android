package artic.edu.search

import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchViewModel @Inject constructor(
        private val analyticsTracker: AnalyticsTracker,
        private val searchService: SearchServiceProvider
) : BaseViewModel() {

    // Subjects for the view-layer to pick up on and hook into

    val searchQuery: Subject<String> = PublishSubject.create()
    val searchSuggestions: Subject<List<String>> = PublishSubject.create()
    val searchResults: Subject<String> = PublishSubject.create()

    init {
    }

    fun clearText() {
        analyticsTracker.reportEvent(ScreenCategoryName.Search, AnalyticsAction.searchAbandoned)
    }

}

