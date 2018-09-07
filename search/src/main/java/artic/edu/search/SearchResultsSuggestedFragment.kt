package artic.edu.search

import kotlin.reflect.KClass

class SearchResultsSuggestedFragment : SearchResultsBaseFragment<SearchResultsSuggestedViewModel>() {
    override val viewModelClass: KClass<SearchResultsSuggestedViewModel> = SearchResultsSuggestedViewModel::class
}