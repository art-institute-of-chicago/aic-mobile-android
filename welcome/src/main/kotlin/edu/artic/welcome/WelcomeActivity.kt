package edu.artic.welcome

import android.content.Intent
import android.os.Bundle
import androidx.navigation.Navigation
import edu.artic.base.utils.NavigationSelectListener
import edu.artic.base.utils.disableShiftMode
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : BaseActivity() {

    override val layoutResId: Int
        get() = R.layout.activity_welcome

    companion object {
        val EXRTA_QUIT: String = "Quit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.extras?.getBoolean(EXRTA_QUIT) == true) {
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
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra(EXRTA_QUIT, true)
                startActivity(intent)
                return
            }
        }
        super.onBackPressed()
    }
}