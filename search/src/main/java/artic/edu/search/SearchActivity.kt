package artic.edu.search

import android.os.Bundle
import android.os.PersistableBundle
import edu.artic.base.utils.disableShiftMode
import edu.artic.navigation.NavigationSelectListener
import edu.artic.viewmodel.BaseViewModelActivity
import kotlinx.android.synthetic.main.activity_search.*
import kotlin.reflect.KClass

class SearchActivity : BaseViewModelActivity<SearchViewModel>() {
    override val viewModelClass: KClass<SearchViewModel>
        get() = SearchViewModel::class

    override val layoutResId: Int
        get() = R.layout.activity_search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomNavigation.disableShiftMode(R.color.menu_color_list)
        bottomNavigation.setOnNavigationItemSelectedListener(NavigationSelectListener(this))
    }
}
