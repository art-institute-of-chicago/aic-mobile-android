package artic.edu.search

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_search_results_sub.*
import kotlin.reflect.KClass

class SearchResultsSuggestedFragment : BaseViewModelFragment<SearchResultsSuggestedViewModel>() {
    override val viewModelClass: KClass<SearchResultsSuggestedViewModel> = SearchResultsSuggestedViewModel::class
    override val title: String = ""
    override val layoutResId: Int = R.layout.fragment_search_results_sub
    override val screenCategory: ScreenCategoryName? = null

    override val overrideStatusBarColor: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resultsRV.adapter = SearchResultsAdapter()
        resultsRV.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
    }

    override fun setupBindings(viewModel: SearchResultsSuggestedViewModel) {
        super.setupBindings(viewModel)

        val adapter = resultsRV.adapter as SearchResultsAdapter

        viewModel.cells
                .bindTo(adapter.itemChanges())
                .disposedBy(disposeBag)

    }

}