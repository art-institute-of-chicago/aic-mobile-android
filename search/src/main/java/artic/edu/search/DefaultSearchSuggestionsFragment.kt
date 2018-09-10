package artic.edu.search


import android.content.Intent
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


class DefaultSearchSuggestionsFragment : SearchResultsBaseFragment<DefaultSearchSuggestionsViewModel>() {
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

    override fun setupNavigationBindings(viewModel: DefaultSearchSuggestionsViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo.subscribeBy { navigation ->
            when (navigation) {
                is Navigate.Forward -> {
                    when (navigation.endpoint) {
                        is DefaultSearchSuggestionsViewModel.NavigationEndpoint.ArticObjectDetails -> {
                            val o = (navigation.endpoint as DefaultSearchSuggestionsViewModel.NavigationEndpoint.ArticObjectDetails).articObject
                            val mapIntent = NavigationConstants.MAP.asDeepLinkIntent().apply {
                                putExtra(ARG_SEARCH_OBJECT, o)
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                            startActivity(mapIntent)
                        }
                    }
                }
                is Navigate.Back -> {

                }
            }
        }.disposedBy(disposeBag)
    }
}
