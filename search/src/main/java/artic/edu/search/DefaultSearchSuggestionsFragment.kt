package artic.edu.search


import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlin.reflect.KClass


class DefaultSearchSuggestionsFragment : BaseViewModelFragment<DefaultSearchSuggestionsViewModel>() {

    override val viewModelClass: KClass<DefaultSearchSuggestionsViewModel>
        get() = DefaultSearchSuggestionsViewModel::class

    override val title = "Search"

    override val layoutResId = R.layout.fragment_default_search_suggestions

    override val screenCategory: ScreenCategoryName?
        get() = ScreenCategoryName.Search

}
