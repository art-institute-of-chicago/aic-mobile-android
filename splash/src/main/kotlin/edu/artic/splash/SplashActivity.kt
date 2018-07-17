package edu.artic.splash

import android.content.Intent
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.main.MainActivity
import edu.artic.viewmodel.BaseViewModelActivity
import edu.artic.viewmodel.Navigate
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

        viewModel.navigateTo
                .subscribe {
                    when (it) {
                        is Navigate.Forward -> {
                            when (it.endpoint) {
                                is SplashViewModel.NavigationEndpoint.Welcome -> {
                                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                                    finish()
                                }
                            }
                        }
                        is Navigate.Back -> {

                        }
                    }
                }.disposedBy(disposeBag)
    }

    override fun onPause() {
        super.onPause()
        disposeBag.clear()
    }
}