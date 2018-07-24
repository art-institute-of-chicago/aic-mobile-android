package edu.artic.base.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import edu.artic.base.R

/**
 * {@link BottomNavigationView.OnNavigationItemSelectedListener} Implementation for app.
 * @author Sameer Dhakal (Fuzz)
 */
class NavigationSelectListener(val context: Context) : BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("artic://edu.artic.home"))
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                context.startActivity(intent)
                false
            }
            R.id.action_map -> {
                false
            }
            R.id.action_audio -> {
                false
            }
            R.id.action_info -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("artic://edu.artic.info"))
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
