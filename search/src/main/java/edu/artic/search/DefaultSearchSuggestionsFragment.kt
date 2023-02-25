package edu.artic.search


import edu.artic.analytics.ScreenName
import edu.artic.search.databinding.FragmentSearchResultsSubBinding
import kotlin.reflect.KClass


class DefaultSearchSuggestionsFragment :
    SearchBaseFragment<FragmentSearchResultsSubBinding, DefaultSearchSuggestionsViewModel>() {
    override val viewModelClass: KClass<DefaultSearchSuggestionsViewModel>
        get() = DefaultSearchSuggestionsViewModel::class

    override val screenName: ScreenName = ScreenName.Search
}
