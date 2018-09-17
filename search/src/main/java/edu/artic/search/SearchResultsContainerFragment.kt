package edu.artic.search

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.widget.TextView
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_search_results.*
import kotlin.reflect.KClass

class SearchResultsContainerFragment : BaseViewModelFragment<SearchResultsContainerViewModel>() {
    override val viewModelClass: KClass<SearchResultsContainerViewModel> = SearchResultsContainerViewModel::class
    override val title: String = ""
    override val layoutResId: Int = R.layout.fragment_search_results
    override val screenCategory: ScreenCategoryName? = ScreenCategoryName.Search

    override val overrideStatusBarColor: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabStrip.setupWithViewPager(viewPager)
        val adapter = SearchResultsPagerAdapter(childFragmentManager)
        viewPager.adapter = adapter
        viewPager.currentItem = 0
        viewPager.offscreenPageLimit = 1
        setupTabStripUi()
    }

    private fun setupTabStripUi() {
        val selectedTypeface = ResourcesCompat.getFont(requireContext(), R.font.ideal_sans_medium)
        val defaultTypeface = ResourcesCompat.getFont(requireContext(), R.font.ideal_sans_light)

        for (i in 0 until tabStrip.tabCount) {
            tabStrip.getTabAt(i)?.let {
                it.setCustomView(R.layout.tab_search_result_container)
                (it.customView as TextView).apply {
                    text = viewPager.adapter?.getPageTitle(i)
                    typeface = if (i == 0) {
                        selectedTypeface
                    } else {
                        defaultTypeface
                    }
                }
            }
        }
        tabStrip.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                (tab?.customView as TextView?)?.typeface = selectedTypeface
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                (tab?.customView as TextView?)?.typeface = defaultTypeface
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                (tab?.customView as TextView?)?.typeface = selectedTypeface
            }

        })
    }
}