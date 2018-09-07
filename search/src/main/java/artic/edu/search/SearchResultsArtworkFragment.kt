package artic.edu.search

import kotlin.reflect.KClass

class SearchResultsArtworkFragment : SearchResultsBaseFragment<SearchResultsArtworkViewModel>() {
    override val viewModelClass: KClass<SearchResultsArtworkViewModel> = SearchResultsArtworkViewModel::class
}