package edu.artic.info

import android.os.Bundle
import edu.artic.base.utils.NavigationSelectListener
import edu.artic.base.utils.disableShiftMode
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_info.*

class InfoActivity : BaseActivity() {

    override val layoutResId: Int
        get() = R.layout.activity_info

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomNavigation.disableShiftMode(R.color.menu_color_list)
        bottomNavigation.selectedItemId = R.id.action_info
        bottomNavigation.setOnNavigationItemSelectedListener(NavigationSelectListener(this))
    }
}
