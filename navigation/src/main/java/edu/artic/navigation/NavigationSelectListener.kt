package edu.artic.navigation

import android.content.Context
import android.content.Intent
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import edu.artic.base.R
import edu.artic.base.utils.asDeepLinkIntent


/**
 * [BottomNavigationView.OnNavigationItemSelectedListener] Implementation for the app.
 *
 * Makes use of [NavigationConstants] and [asDeepLinkIntent] to launch other modules
 * without needing to know [precisely what would be loaded][android.content.ComponentName].
 *
 * @author Sameer Dhakal (Fuzz)
 * @see edu.artic.base.utils.preventReselection
 */
class NavigationSelectListener(val context: Context) : BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                val intent = NavigationConstants.HOME.asDeepLinkIntent()
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION
                context.startActivity(intent)
                false
            }
            R.id.action_map -> {
                val intent = NavigationConstants.MAP.asDeepLinkIntent()
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                context.startActivity(intent)
                false
            }
            R.id.action_audio -> {
                val intent = NavigationConstants.AUDIO.asDeepLinkIntent()
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                context.startActivity(intent)
                false
            }
            R.id.action_info -> {
                val intent = NavigationConstants.INFO.asDeepLinkIntent()
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                context.startActivity(intent)
                false
            }
            else -> {
                false
            }
        }
    }

}
