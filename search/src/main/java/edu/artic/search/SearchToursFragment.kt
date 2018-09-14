package edu.artic.search

import kotlin.reflect.KClass

class SearchToursFragment : SearchBaseFragment<SearchToursViewModel>() {
    override val viewModelClass: KClass<SearchToursViewModel> = SearchToursViewModel::class
}