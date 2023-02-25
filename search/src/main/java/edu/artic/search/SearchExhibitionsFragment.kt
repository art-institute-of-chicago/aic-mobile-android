package edu.artic.search

import edu.artic.search.databinding.FragmentSearchResultsSubBinding
import kotlin.reflect.KClass

class SearchExhibitionsFragment : SearchBaseFragment<FragmentSearchResultsSubBinding,SearchExhibitionsViewModel>() {
    override val viewModelClass: KClass<SearchExhibitionsViewModel> = SearchExhibitionsViewModel::class

}