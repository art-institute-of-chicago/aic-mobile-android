package edu.artic.media.ui

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.base.utils.setWindowFlag
import edu.artic.viewmodel.BaseViewModelActivity
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.activity_audio_tutorial.*
import kotlin.reflect.KClass

class AudioTutorialActivity : BaseViewModelActivity<AudioTutorialViewModel>() {
    override val viewModelClass: KClass<AudioTutorialViewModel> = AudioTutorialViewModel::class
    override val layoutResId: Int = R.layout.activity_audio_tutorial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        window?.statusBarColor = Color.TRANSPARENT

        ok
                .clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onOkClicked()
                }.disposedBy(disposeBag)
    }


    override fun onStart() {
        super.onStart()
        viewModel.navigateTo
                .subscribe {
                    when (it) {
                        is Navigate.Forward -> {
                            when (it.endpoint) {
                                AudioTutorialViewModel.NavigationEndpoint.Finish -> finish()
                            }
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
