package artic.edu.search

import edu.artic.viewmodel.BaseViewModelActivity
import kotlin.reflect.KClass

class SearchActivity : BaseViewModelActivity<SearchViewModel>() {
    override val viewModelClass: KClass<SearchViewModel>
        get() = SearchViewModel::class

    override val layoutResId: Int
        get() = R.layout.activity_search

}
