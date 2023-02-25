package edu.artic.search

import edu.artic.search.databinding.FragmentSearchResultsSubBinding
import kotlin.reflect.KClass

class SearchToursFragment : SearchBaseFragment<FragmentSearchResultsSubBinding,SearchToursViewModel>() {
    override val viewModelClass: KClass<SearchToursViewModel> = SearchToursViewModel::class
}