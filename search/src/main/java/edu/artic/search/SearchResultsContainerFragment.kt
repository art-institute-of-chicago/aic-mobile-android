package edu.artic.search

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.fuzz.rx.disposedBy
import com.google.android.material.tabs.TabLayout
import edu.artic.analytics.ScreenName
import edu.artic.search.databinding.FragmentSearchResultsBinding
import edu.artic.viewmodel.BaseViewModelFragment
import kotlin.reflect.KClass

class SearchResultsContainerFragment :
    BaseViewModelFragment<FragmentSearchResultsBinding, SearchResultsContainerViewModel>() {
    override val viewModelClass: KClass<SearchResultsContainerViewModel> =
        SearchResultsContainerViewModel::class
    override val title = R.string.global_empty_string

    override val screenName: ScreenName? = ScreenName.Search

    override val overrideStatusBarColor: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tabStrip.setupWithViewPager(binding.viewPager)
        val adapter = SearchResultsPagerAdapter(childFragmentManager, requireContext())
        binding.viewPager.apply {
            this.adapter = adapter
            this.currentItem = 0
            this.offscreenPageLimit = 1
        }
        setupTabStripUi()

    }

    private fun setupTabStripUi() {
        val selectedTypeface = ResourcesCompat.getFont(requireContext(), R.font.ideal_sans_medium)
        val defaultTypeface = ResourcesCompat.getFont(requireContext(), R.font.ideal_sans_light)

        for (i in 0 until binding.tabStrip.tabCount) {
            binding.tabStrip.getTabAt(i)?.let {
                it.setCustomView(R.layout.tab_search_result_container)
                (it.customView as TextView).apply {
                    text = binding.viewPager.adapter?.getPageTitle(i)
                    typeface = if (it.isSelected) {
                        selectedTypeface
                    } else {
                        defaultTypeface
                    }
                }
            }
        }
        setupOnTabChangedListener(selectedTypeface, defaultTypeface)

    }

    private fun setupOnTabChangedListener(selectedTypeface: Typeface?, defaultTypeface: Typeface?) {
        binding.tabStrip.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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

    override fun setupBindings(viewModel: SearchResultsContainerViewModel) {
        super.setupBindings(viewModel)
        viewModel
            .currentlySelectedPage
            .distinctUntilChanged()
            .subscribe {
                if (binding.viewPager.currentItem != it) {
                    binding.viewPager.currentItem = it
                }
            }
            .disposedBy(disposeBag)
        binding.viewPager.addOnPageChangeListener(object :
            androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                viewModel.onPageChanged(position)
            }
        })
    }
}