package edu.artic.base.utils

import android.view.MenuItem
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView


/**
 * Special implementation of [BottomNavigationView.OnNavigationItemReselectedListener]
 * with absolutely no state and no action. Set it on a view with [preventReselection].
 */
private object IgnoreReselection : BottomNavigationView.OnNavigationItemReselectedListener {
    // No need to do anything in the method body.
    override fun onNavigationItemReselected(item: MenuItem) = Unit
}

/**
 * Disable on-click events for highlighted items.
 *
 * See [BottomNavigationView.setOnNavigationItemReselectedListener] for
 * details.
 */
fun BottomNavigationView.preventReselection() {
    setOnNavigationItemReselectedListener(IgnoreReselection)
}

/**
 * Function for showing/hiding the view.
 */
fun View.show(show: Boolean) {
    if (show) {
        this.visibility = View.VISIBLE
    } else {
        this.visibility = View.GONE
    }
}