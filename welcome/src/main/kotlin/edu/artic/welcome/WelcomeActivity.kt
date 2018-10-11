package edu.artic.welcome

import android.os.Bundle
import android.transition.Explode
import android.transition.Fade
import android.view.Window
import edu.artic.base.utils.disableShiftMode
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : BaseActivity() {

    override val layoutResId: Int
        get() = R.layout.activity_welcome

    override fun onCreate(savedInstanceState: Bundle?) {
        with(window) {
            requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            allowEnterTransitionOverlap = true
            enterTransition = Explode()
            exitTransition = Fade()
        }

        super.onCreate(savedInstanceState)

        bottomNavigation.apply {
            disableShiftMode(R.color.menu_color_list)
            selectedItemId = R.id.action_home
            setOnNavigationItemReselectedListener {
                navController.popBackStack(R.id.welcomeFragment, false)
            }
            setOnNavigationItemSelectedListener(NavigationSelectListener(this@WelcomeActivity))
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            if (navController.currentDestination?.id == R.id.welcomeFragment) {
                finishAffinity()
                overridePendingTransition(0, 0)
                return
            }
        }
        super.onBackPressed()
    }
}