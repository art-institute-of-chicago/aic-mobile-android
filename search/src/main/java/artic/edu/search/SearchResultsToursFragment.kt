package artic.edu.search

import kotlin.reflect.KClass

class SearchResultsToursFragment : SearchResultsBaseFragment<SearchResultsToursViewModel>() {
    override val viewModelClass: KClass<SearchResultsToursViewModel> = SearchResultsToursViewModel::class
}