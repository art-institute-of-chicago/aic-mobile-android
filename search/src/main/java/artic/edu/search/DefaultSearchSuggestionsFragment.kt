package artic.edu.search


import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_default_search_suggestions.*
import kotlin.reflect.KClass


class DefaultSearchSuggestionsFragment : BaseViewModelFragment<DefaultSearchSuggestionsViewModel>() {

    companion object {
        const val MAX_ARTWORKS_PER_ROW: Int = 5
    }

    override val viewModelClass: KClass<DefaultSearchSuggestionsViewModel>
        get() = DefaultSearchSuggestionsViewModel::class

    override val title = "Search"

    override val layoutResId = R.layout.fragment_default_search_suggestions

    override val screenCategory: ScreenCategoryName?
        get() = ScreenCategoryName.Search

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        items.apply {
            layoutManager = GridLayoutManager(view.context, MAX_ARTWORKS_PER_ROW, GridLayoutManager.VERTICAL, false)
            (layoutManager as GridLayoutManager).spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return (adapter as DefaultSuggestionAdapter).getSpanCount(position)
                }
            }
            adapter = DefaultSuggestionAdapter()
        }
    }

    override fun setupBindings(viewModel: DefaultSearchSuggestionsViewModel) {
        super.setupBindings(viewModel)
        val adapter = items.adapter as DefaultSuggestionAdapter
        viewModel.cells
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

    }
}
