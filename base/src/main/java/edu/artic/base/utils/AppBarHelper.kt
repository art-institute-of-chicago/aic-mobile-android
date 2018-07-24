package edu.artic.base.utils

import android.support.design.widget.AppBarLayout
import android.widget.TextView

class AppBarHelper {
    companion object {
        fun updateDetailTitle(appBarLayout: AppBarLayout, verticalOffset : Int, expandedTitle: TextView, toolbarTitle: TextView) {
            val progress: Double = 1 - Math.abs(verticalOffset) / appBarLayout.totalScrollRange.toDouble()
            if (progress <= .5) {
                val diff = .5f - progress.toFloat()
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
    }
}