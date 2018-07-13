package edu.artic.splash

import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.viewmodel.BaseViewModelActivity
import kotlinx.android.synthetic.main.activity_splash.*
import kotlin.reflect.KClass

class SplashActivity : BaseViewModelActivity<SplashViewModel>() {
    override val layoutResId: Int
        get() = R.layout.activity_splash

    override val viewModelClass: KClass<SplashViewModel>
        get() = SplashViewModel::class

    override fun onResume() {
        super.onResume()
        viewModel.percentage
                .map { "Percentage : ${it * 100}" }
                .bindToMain(percentText.text())
                .disposedBy(disposeBag)
    }

    override fun onPause() {
        super.onPause()
        disposeBag.clear()
    }
}