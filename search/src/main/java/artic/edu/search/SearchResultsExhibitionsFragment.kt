package artic.edu.search

import kotlin.reflect.KClass

class SearchResultsExhibitionsFragment : SearchResultsBaseFragment<SearchResultsExhibitionsViewModel>() {
    override val viewModelClass: KClass<SearchResultsExhibitionsViewModel> = SearchResultsExhibitionsViewModel::class

}