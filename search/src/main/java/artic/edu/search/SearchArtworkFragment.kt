package artic.edu.search

import kotlin.reflect.KClass

class SearchArtworkFragment : SearchBaseFragment<SearchArtworkViewModel>() {
    override val viewModelClass: KClass<SearchArtworkViewModel> = SearchArtworkViewModel::class
}