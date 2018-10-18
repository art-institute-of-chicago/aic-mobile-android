package edu.artic.details

import edu.artic.analytics.ScreenName
import edu.artic.ui.BaseFragment

class EmptyDetailsFragment: BaseFragment() {
    override val title: Int
        get() = R.string.noTitle
    override val layoutResId: Int
        get() = R.layout.fragment_empty_details
    override val screenName: ScreenName?
        get() = null


}