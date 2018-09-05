package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.textChanges
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.search_app_bar_layout.view.*
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

        toolbar!!.searchEditText
                .textChanges()
                .filter { it.isNotEmpty() }
                .map { it.toString() }
                .bindTo(viewModel.searchQuery)
                .disposedBy(disposeBag)

        // TODO: bind viewModel.searchSuggestions, viewModel.searchResults, etc.
    }
}