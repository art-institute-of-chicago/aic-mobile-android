package artic.edu.search

import android.os.Bundle
import android.view.View
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_search_results.*
import kotlin.reflect.KClass

class SearchResultsFragment : BaseViewModelFragment<SearchResultsViewModel>() {
    override val viewModelClass: KClass<SearchResultsViewModel> = SearchResultsViewModel::class
    override val title: String = ""
    override val layoutResId: Int = R.layout.fragment_search_results
    override val screenCategory: ScreenCategoryName? = ScreenCategoryName.Search

    override val overrideStatusBarColor: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SearchResultsPagerAdapter(childFragmentManager)
        viewPager.adapter = adapter
        viewPager.currentItem = 0
        viewPager.offscreenPageLimit = 1
    }
}