package edu.artic.welcome

import android.os.Bundle
import android.view.View
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_welcome.*
import kotlin.reflect.KClass

class WelcomeFragment : BaseViewModelFragment<WelcomeViewModel>() {

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override val layoutResId: Int
        get() = R.layout.fragment_welcome

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val progress: Double = 1 - Math.abs(verticalOffset) / appBarLayout!!.totalScrollRange.toDouble()
            searchIcon.background.alpha = (progress * 255).toInt()
            flagIcon.drawable.alpha = (progress * 255).toInt()
        }
    }
}
