package edu.artic.welcome

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.view.View
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.welcome.R.id.appBarLayout
import kotlinx.android.synthetic.main.app_bar_layout.view.*
import kotlinx.android.synthetic.main.fragment_welcome.*
import kotlin.reflect.KClass

class WelcomeFragment : BaseViewModelFragment<WelcomeViewModel>() {
    override val title: String
        get() = "Welcome"

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override val layoutResId: Int
        get() = R.layout.fragment_welcome

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * TODO:: move this logic away into the app bar view class
         * TODO:: Make a Custom AppBar view that dynamically switches the toolbar type (collapsible and non collapsible)
         */
        val appBar = appBarLayout as AppBarLayout
        (appBarLayout as AppBarLayout).apply {
            addOnOffsetChangedListener { aBarLayout, verticalOffset ->
                val progress: Double = 1 - Math.abs(verticalOffset) / aBarLayout.totalScrollRange.toDouble()
                appBar.searchIcon.background.alpha = (progress * 255).toInt()
                appBar.flagIcon.drawable.alpha = (progress * 255).toInt()
            }
        }

    }
}
