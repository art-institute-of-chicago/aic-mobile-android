package artic.edu.search


import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.navigation.NavigationConstants
import edu.artic.navigation.NavigationConstants.Companion.ARG_SEARCH_OBJECT
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_search_results_sub.*
import kotlin.reflect.KClass


class DefaultSearchSuggestionsFragment : SearchBaseFragment<DefaultSearchSuggestionsViewModel>() {
    override val viewModelClass: KClass<DefaultSearchSuggestionsViewModel>
        get() = DefaultSearchSuggestionsViewModel::class

    override val screenCategory: ScreenCategoryName? = ScreenCategoryName.Search

    override fun setupBindings(viewModel: DefaultSearchSuggestionsViewModel) {
        super.setupBindings(viewModel)
        val adapter = resultsRV.adapter as SearchResultsAdapter

        adapter.itemClicksWithPosition()
                .subscribeBy { (pos, searchViewModel) ->
                    viewModel.onClickItem(pos, searchViewModel)
                }.disposedBy(disposeBag)

        viewModel.cells
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)
    }
}
