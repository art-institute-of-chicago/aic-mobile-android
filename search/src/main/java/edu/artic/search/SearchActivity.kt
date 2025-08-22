package edu.artic.search

import edu.artic.search.databinding.ActivitySearchBinding
import edu.artic.viewmodel.BaseViewModelActivity
import kotlin.reflect.KClass

/**
 * Try to avoid using this class. Most callers should be fine with adding a
 * [SearchFragment] to a pre-existing activity.
 *
 * Launched by `edu.artic.navigation.NavigationConstants.SEARCH`.
 */
class SearchActivity : BaseViewModelActivity<ActivitySearchBinding, SearchViewModel>() {
    override val viewModelClass: KClass<SearchViewModel>
        get() = SearchViewModel::class

}
