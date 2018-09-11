package artic.edu.search

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.navigation.Navigation
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.search_app_bar_layout.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class SearchFragment : BaseViewModelFragment<SearchViewModel>() {

    lateinit var textChangesDisposable: Disposable

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

        searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm: InputMethodManager = requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromInputMethod(v.windowToken, 0)
                viewModel.onClickSearch()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false

        }

        bindSearchText()

        viewModel.searchText
                .subscribe {
                    if (searchEditText.text.toString() != it) {
                        // It is important to dispose here as otherwise we would get into
                        // an infinte loop of text updated, update edit text,
                        // notify edit text updated etc etc
                        textChangesDisposable.dispose()
                        searchEditText.setText(it)
                        searchEditText.setSelection(it.length)
                        bindSearchText()
                    }
                }.disposedBy(disposeBag)

        viewModel.closeButtonVisible
                .bindToMain(close.visibility())
                .disposedBy(disposeBag)

        viewModel.shouldClearTextInput
                .distinctUntilChanged()
                .filter { it }
                .subscribe {
                    searchEditText.setText("")
                }.disposedBy(disposeBag)

        close.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onCloseClicked()
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
                            navController.popBackStack(R.id.defaultSearchSuggestionsFragment, false)
                        }
                        SearchViewModel.NavigationEndpoint.DynamicSearchResults -> {
                            navController.navigate(R.id.goToSearchResults)
                        }
                    }
                }
                .disposedBy(disposeBag)
    }

    private fun bindSearchText() {
        textChangesDisposable = searchEditText
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
}