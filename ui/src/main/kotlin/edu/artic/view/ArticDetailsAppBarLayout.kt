package edu.artic.view

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import edu.artic.base.utils.updateDetailTitle
import edu.artic.ui.R
import kotlinx.android.synthetic.main.view_details_app_bar.view.*

/**
 * Description: Consolidates details functionality and layout for use in details pages.
 */
class ArticDetailsAppBarLayout(context: Context, attrs: AttributeSet? = null) : AppBarLayout(context, attrs) {

    /**
     * Currently, kotlin extensions for Android Views do not work cross modules. So we expose a
     * property here that maps to the internal property.
     */
    val detailImage: ImageView
        get() = image

    init {
        View.inflate(context, R.layout.view_details_app_bar, this)
        fitsSystemWindows = true
        setBackgroundResource(android.R.color.transparent)

        // update our content when offset changes.
        addOnOffsetChangedListener { _, verticalOffset ->
            updateDetailTitle(verticalOffset, expandedTitle, toolbarTitle)
        }
    }

    fun setImageTransitionName(name: String) {
        image.transitionName = name
    }

    fun setTitleText(text: CharSequence) {
        expandedTitle.text = text
        toolbarTitle.text = text
    }
}