package artic.edu.search

import kotlin.reflect.KClass

class SearchSuggestedFragment : SearchBaseFragment<SearchSuggestedViewModel>() {
    override val viewModelClass: KClass<SearchSuggestedViewModel> = SearchSuggestedViewModel::class
}