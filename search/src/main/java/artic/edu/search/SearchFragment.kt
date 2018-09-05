package artic.edu.search

import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
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
}