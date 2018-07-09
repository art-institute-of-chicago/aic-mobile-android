package edu.artic.splash

import android.os.Bundle
import edu.artic.viewmodel.BaseViewModelActivity
import kotlin.reflect.KClass

class SplashActivity : BaseViewModelActivity<SplashViewModel>(){
    override val viewModelClass: KClass<SplashViewModel>
        get() = SplashViewModel::class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }
}