package edu.artic.search

import kotlin.reflect.KClass

class SearchSuggestedFragment : SearchBaseFragment<SearchSuggestedViewModel>() {
    override val viewModelClass: KClass<SearchSuggestedViewModel> = SearchSuggestedViewModel::class

    override fun setupBindings(viewModel: SearchSuggestedViewModel) {
        super.setupBindings(viewModel)
        parentFragment?.let {
            if(it is SearchResultsContainerFragment) {
                viewModel.parentViewModel = it.viewModel
            }
        }
    }
}