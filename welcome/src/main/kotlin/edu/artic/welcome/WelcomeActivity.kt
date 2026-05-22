package edu.artic.welcome

import android.os.Bundle
import android.transition.Fade
import android.view.Window
import edu.artic.navigation.NavigationSelectListener
import edu.artic.navigation.overrideTransition
import edu.artic.ui.BaseActivity
import edu.artic.welcome.databinding.ActivityWelcomeBinding

//import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {


    override fun onCreate(savedInstanceState: Bundle?) {
        with(window) {
            requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            allowEnterTransitionOverlap = true
            enterTransition = Fade()
            exitTransition = Fade()
        }

        super.onCreate(savedInstanceState)

        binding.bottomNavigation.apply {
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
                overrideTransition()
                return
            }
        }
        super.onBackPressed()
    }
}