package edu.artic.splash

import edu.artic.viewmodel.BaseViewModelActivity
import kotlin.reflect.KClass

class SplashActivity : BaseViewModelActivity<SplashViewModel>() {
    override val layoutResId: Int
        get() = R.layout.activity_splash

    override val viewModelClass: KClass<SplashViewModel>
        get() = SplashViewModel::class
}