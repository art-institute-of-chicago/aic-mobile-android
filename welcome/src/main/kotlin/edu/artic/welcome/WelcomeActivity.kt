package edu.artic.welcome

import android.os.Bundle
import androidx.navigation.Navigation
import edu.artic.base.utils.disableShiftMode
import edu.artic.base.utils.quitIntent
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : BaseActivity() {

    companion object {
        val EXTRA_QUIT: String = "EXTRA_QUIT"
    }

    override val layoutResId: Int
        get() = R.layout.activity_welcome

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.extras?.getBoolean(EXTRA_QUIT) == true) {
            finish()
            return
        }
        bottomNavigation.disableShiftMode(R.color.menu_color_list)
        bottomNavigation.selectedItemId = R.id.action_home
        bottomNavigation.setOnNavigationItemSelectedListener(NavigationSelectListener(this))
    }

    override fun onBackPressed() {
        if (!isTaskRoot && supportFragmentManager.backStackEntryCount == 0) {
            val navigationController = Navigation.findNavController(this, R.id.container)
            if (navigationController.currentDestination.id == R.id.welcomeFragment) {
                val intent = quitIntent(recipient = WelcomeActivity::class.java)
                intent.putExtra(EXTRA_QUIT, true)
                startActivity(intent)
                finish()
                return
            }
        }
        super.onBackPressed()
    }
}