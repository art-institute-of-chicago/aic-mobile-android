package artic.edu.search

import android.os.Bundle
import android.support.v7.widget.Toolbar
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.textChanges
import edu.artic.base.utils.disableShiftMode
import edu.artic.navigation.NavigationSelectListener
import edu.artic.viewmodel.BaseViewModelActivity
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.search_app_bar_layout.view.*
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
        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        toolbar.searchEditText
                .textChanges()
                .skipInitialValue()
                .map { chars -> chars.count() > 0 }
                .bindTo(toolbar.close.visibility())
                .disposedBy(disposeBag)

        toolbar.close.setOnClickListener {
            toolbar.searchEditText.text.clear()
        }


    }
}
