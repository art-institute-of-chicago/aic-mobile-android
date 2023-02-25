package edu.artic.search

import edu.artic.search.databinding.FragmentSearchResultsSubBinding
import kotlin.reflect.KClass

class SearchArtworkFragment :
    SearchBaseFragment<FragmentSearchResultsSubBinding, SearchArtworkViewModel>() {
    override val viewModelClass: KClass<SearchArtworkViewModel> = SearchArtworkViewModel::class
}