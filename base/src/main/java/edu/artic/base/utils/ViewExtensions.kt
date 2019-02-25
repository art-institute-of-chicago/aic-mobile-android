package edu.artic.base.utils

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.support.design.internal.BottomNavigationItemView
import android.support.design.internal.BottomNavigationMenuView
import android.support.design.widget.BottomNavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import edu.artic.base.R


/**
 * This hack is required for displaying title below icon.
 * There is an api available to achieve this in support library 28-alpha1 (labelVisibilityMode mode).
 * Once support library goes stable we can remove this hack.
 *
 * @see https://stackoverflow.com/a/47407229/4253091
 * @param colorList ColorStateList for icon and text
 */
fun BottomNavigationView.disableShiftMode(colorList: Int = 0) {
    val menuView = getChildAt(0) as BottomNavigationMenuView

    // While we're adjusting the other properties, we can normalize the font in use too.
    val type: Typeface? = ResourcesCompat.getFont(context, R.font.ideal_sans_semibold)

    @SuppressLint("RestrictedApi")
    for (i in 0 until menuView.childCount) {
        (menuView.getChildAt(i) as BottomNavigationItemView).apply {
            setChecked(false)
            if (type != null) {
                overrideLabelFont(type)
            }
            if (colorList > 0) {
                setIconTintList(ContextCompat.getColorStateList(this.context, colorList))
                setTextColor(ContextCompat.getColorStateList(this.context, colorList))
            }
        }
    }
}

/**
 * There's no other API for defining these font styles. We just
 * find and set Typeface on the `smallLabel` and 'largeLabel`
 * directly.
 */
private fun BottomNavigationItemView.overrideLabelFont(font: Typeface) {
    findViewById<TextView?>(R.id.smallLabel)?.typeface = font
    findViewById<TextView?>(R.id.largeLabel)?.typeface = font
}

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
fun View.show(show: Boolean){
    if (show) {
        this.visibility = View.VISIBLE
    } else {
        this.visibility = View.GONE
    }
}