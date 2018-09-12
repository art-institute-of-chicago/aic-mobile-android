package edu.artic.splash

import android.os.Bundle
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelActivity
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.activity_splash.*
import kotlin.reflect.KClass

class SplashActivity : BaseViewModelActivity<SplashViewModel>() {
    override val layoutResId: Int
        get() = R.layout.activity_splash

    override val viewModelClass: KClass<SplashViewModel>
        get() = SplashViewModel::class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.percentage
                .map {
                    "Percentage : %.2f".format(it * 100)
                }
                .onErrorReturn {
                    it.localizedMessage
                }
                .bindToMain(percentText.text())
                .disposedBy(disposeBag)
    }

    override fun onStart() {
        super.onStart()
        viewModel.navigateTo
                .subscribe {
                    when (it) {
                        is Navigate.Forward -> {
                            when (it.endpoint) {
                                is SplashViewModel.NavigationEndpoint.Welcome -> {
                                    val intent = NavigationConstants.HOME.asDeepLinkIntent()
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        }
                        is Navigate.Back -> {

                        }
                    }
                }.disposedBy(navDisposeBag)
    }

    override fun onStop() {
        super.onStop()
        navDisposeBag.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposeBag.clear()
    }
}