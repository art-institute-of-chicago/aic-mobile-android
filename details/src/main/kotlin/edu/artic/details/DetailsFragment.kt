package edu.artic.details

import edu.artic.analytics.ScreenCategoryName
import edu.artic.ui.BaseFragment

class DetailsFragment: BaseFragment() {
    override val title: Int
        get() = R.string.noTitle
    override val layoutResId: Int
        get() = R.layout.fragment_details
    override val screenCategory: ScreenCategoryName?
        get() = null


}