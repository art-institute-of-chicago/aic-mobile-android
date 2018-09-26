package edu.artic.search

import edu.artic.viewmodel.BaseViewModelActivity
import kotlin.reflect.KClass

/**
 * Try to avoid using this class. Most callers should be fine with adding a
 * [SearchFragment] to a pre-existing activity.
 *
 * Launched by `edu.artic.navigation.NavigationConstants.SEARCH`.
 */
class SearchActivity : BaseViewModelActivity<SearchViewModel>() {
    override val viewModelClass: KClass<SearchViewModel>
        get() = SearchViewModel::class

    override val layoutResId: Int
        get() = R.layout.activity_search

}
