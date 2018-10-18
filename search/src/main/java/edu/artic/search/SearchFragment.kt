package edu.artic.search

import android.view.inputmethod.EditorInfo
import androidx.navigation.Navigation
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import edu.artic.analytics.ScreenName
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.search_app_bar_layout.*
import kotlin.reflect.KClass

/**
 * One of the primary fragments of the app. This does not have its own entry in
 * the bottom navigation bar, unlike `WelcomeActivity`, `AudioActivity`,
 * `MapActivity`, and `InfoActivity`. Instead, an instance may appear under any
 * of those sections.
 *
 * Callers should not normally need to load this into a [SearchActivity].
 *
 * This class is backed by a single [SearchViewModel], which in turn delegates
 * some important functions to a global [SearchResultsManager].
 *
 * See [bindSearchText] for a little more info about how the input events are
 * hooked up.
*/
class SearchFragment : BaseViewModelFragment<SearchViewModel>() {

    lateinit var textChangesDisposable: Disposable

    override val viewModelClass: KClass<SearchViewModel>
        get() = SearchViewModel::class
    override val title = R.string.noTitle
    override val layoutResId: Int
        get() = R.layout.fragment_search
    override val screenName: ScreenName?
        get() = null

    override fun hasTransparentStatusBar(): Boolean = false

    override val customToolbarColorResource: Int
        get() = R.color.greyText

    override fun setupBindings(viewModel: SearchViewModel) {
        super.setupBindings(viewModel)

        searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.onClickSearch()
            }
            return@setOnEditorActionListener false

        }

        bindSearchText()

        viewModel.searchText
                .subscribe {
                    if (searchEditText.text.toString() != it) {
                        // It is important to dispose here as otherwise we would get into
                        // an infinite loop of text updated, update edit text,
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

    /**
     * The [android.widget.EditText] which provides the text
     *
     * 1. comes from this fragment's layout
     * 2. is inflated inside the [toolbar]
     * 3. must default to being empty (we skip the initial value under that assumption)
     */
    private fun bindSearchText() {
        textChangesDisposable = searchEditText
                .afterTextChangeEvents()
                .skipInitialValue()
                .subscribe { event ->
                    event.editable()?.let { editable ->
                        viewModel.onTextChanged(editable.toString())
                    }
                }
                .disposedBy(disposeBag)
    }

    override fun onBackPressed(): Boolean {
        return if (searchEditText.text.isNotEmpty()) {
            viewModel.onTextChanged("")
            true
        } else {
            false
        }
    }
}