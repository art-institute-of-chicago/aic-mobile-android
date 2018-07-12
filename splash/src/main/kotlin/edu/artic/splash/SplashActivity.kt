package edu.artic.splash

import android.content.Intent
import android.os.Handler
import edu.artic.main.MainActivity
import edu.artic.viewmodel.BaseViewModelActivity
import kotlin.reflect.KClass

class SplashActivity : BaseViewModelActivity<SplashViewModel>() {
    override val layoutResId: Int
        get() = R.layout.activity_splash

    override val viewModelClass: KClass<SplashViewModel>
        get() = SplashViewModel::class

    override fun onViewModelCreated(viewModel: SplashViewModel) {
        super.onViewModelCreated(viewModel)
        val handler = Handler()

        /**
         * TODO:: Remove the post delay once splash is functional.
         */
        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }, 1000)
    }
}