package artic.edu.search

import androidx.navigation.Navigation
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.search_app_bar_layout.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class SearchFragment : BaseViewModelFragment<SearchViewModel>() {

    override val viewModelClass: KClass<SearchViewModel>
        get() = SearchViewModel::class
    override val title: String
        get() = ""
    override val layoutResId: Int
        get() = R.layout.fragment_search
    override val screenCategory: ScreenCategoryName?
        get() = null

    override fun hasTransparentStatusBar(): Boolean = false

    override val customToolbarColorResource: Int
        get() = R.color.greyText

    override fun setupBindings(viewModel: SearchViewModel) {
        super.setupBindings(viewModel)
        searchEditText
                .afterTextChangeEvents()
                .skipInitialValue()
                .throttleLast(250, TimeUnit.MILLISECONDS)
                .subscribe { event ->
                    event.editable()?.let { editable ->
                        viewModel.onTextChanged(editable.toString())
                    }
                }
                .disposedBy(disposeBag)


    }

    override fun setupNavigationBindings(viewModel: SearchViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .filterFlatMap({ it is Navigate.Forward }, { it as Navigate.Forward })
                .map { it.endpoint }
                .distinctUntilChanged()
                .subscribe {
                    val navController = Navigation.findNavController(requireActivity(), R.id.searchContainer)
                    when (it) {
                        SearchViewModel.NavigationEndpoint.DefaultSearchResults -> {
                            navController.popBackStack(R.id.defaultSearchSuggestionsFragment, true)
                        }
                        SearchViewModel.NavigationEndpoint.DynamicSearchResults -> {
                            navController.navigate(R.id.goToSearchResults)
                        }
                    }
                }
                .disposedBy(disposeBag)
    }
}