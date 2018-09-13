package artic.edu.search


import edu.artic.analytics.ScreenCategoryName
import kotlin.reflect.KClass


class DefaultSearchSuggestionsFragment : SearchBaseFragment<DefaultSearchSuggestionsViewModel>() {
    override val viewModelClass: KClass<DefaultSearchSuggestionsViewModel>
        get() = DefaultSearchSuggestionsViewModel::class

    override val screenCategory: ScreenCategoryName? = ScreenCategoryName.Search
}
