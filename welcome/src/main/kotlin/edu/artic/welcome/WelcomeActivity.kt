package edu.artic.welcome

import edu.artic.viewmodel.BaseViewModelActivity
import kotlin.reflect.KClass

class WelcomeActivity : BaseViewModelActivity<WelcomeViewModel>() {

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override val layoutResId: Int
        get() = R.layout.activity_welcome

}
