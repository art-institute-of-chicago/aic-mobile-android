package edu.artic.details

import edu.artic.analytics.ScreenName
import edu.artic.details.databinding.FragmentEmptyDetailsBinding
import edu.artic.ui.BaseFragment

class EmptyDetailsFragment : BaseFragment<FragmentEmptyDetailsBinding>() {
    override val title: Int
        get() = R.string.global_empty_string

    override val screenName: ScreenName?
        get() = null


}