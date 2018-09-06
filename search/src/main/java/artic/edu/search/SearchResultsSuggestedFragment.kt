package artic.edu.search

import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlin.reflect.KClass

class SearchResultsSuggestedFragment : BaseViewModelFragment<SearchResultsSuggestedViewModel>() {
    override val viewModelClass: KClass<SearchResultsSuggestedViewModel> = SearchResultsSuggestedViewModel::class
    override val title: String = ""
    override val layoutResId: Int = R.layout.fragment_search_results_sub
    override val screenCategory: ScreenCategoryName? = null

}