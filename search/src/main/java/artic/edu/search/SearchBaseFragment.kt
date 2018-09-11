package artic.edu.search

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_search_results_sub.*

abstract class SearchBaseFragment<TViewModel : SearchBaseViewModel<*>> : BaseViewModelFragment<TViewModel>() {
    override val title: String = ""
    override val layoutResId: Int = R.layout.fragment_search_results_sub
    override val screenCategory: ScreenCategoryName? = null

    override val overrideStatusBarColor: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lm = GridLayoutManager(view.context, 5)
        lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return (resultsRV.adapter as SearchResultsAdapter).getSpanCount(position)
            }
        }

        resultsRV.apply {
            adapter = SearchResultsAdapter()
            layoutManager = lm
            addItemDecoration(SearchDividerItemDecoration(this.context))
        }

    }

    override fun setupBindings(viewModel: TViewModel) {
        super.setupBindings(viewModel)

        val adapter = resultsRV.adapter as SearchResultsAdapter

        viewModel.cells
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

    }

}