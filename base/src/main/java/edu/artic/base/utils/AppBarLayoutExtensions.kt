package edu.artic.base.utils

import android.support.design.widget.AppBarLayout
import android.widget.TextView

fun AppBarLayout.updateDetailTitle(verticalOffset : Int, expandedTitle: TextView, toolbarTitle: TextView) {
    val progress: Double = 1 - Math.abs(verticalOffset) / this.totalScrollRange.toDouble()
    //we only start fading after half the distance has passed
    if (progress <= .5) {
        val diff = .5f - progress.toFloat()
        //if less than 25% of the whole distance is left we start fading in/out the expanded title
        // otherwise we fade in/out the toolbar title
        if (diff <= .25f) {
            toolbarTitle.alpha = 0f
            expandedTitle.alpha = 1 - (diff / .25f)
        } else {
            expandedTitle.alpha = 0f
            toolbarTitle.alpha = (diff / .25f) - 1f
        }
    } else {
        toolbarTitle.alpha = 0f
        expandedTitle.alpha = 1f
    }
}