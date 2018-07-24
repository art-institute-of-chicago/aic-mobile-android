package edu.artic.welcome

import android.os.Bundle
import edu.artic.base.utils.NavigationSelectListener
import edu.artic.base.utils.disableShiftMode
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : BaseActivity() {


    override val layoutResId: Int
        get() = R.layout.activity_welcome

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomNavigation.disableShiftMode(R.color.menu_color_list)
        bottomNavigation.selectedItemId = R.id.action_home
        bottomNavigation.setOnNavigationItemSelectedListener(NavigationSelectListener(this))
    }
}