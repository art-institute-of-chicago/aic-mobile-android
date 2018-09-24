package edu.artic.splash

import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import com.fuzz.rx.disposedBy
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.setWindowFlag
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelActivity
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_splash.*
import kotlin.reflect.KClass

class SplashActivity : BaseViewModelActivity<SplashViewModel>(), TextureView.SurfaceTextureListener {

    private var mMediaPlayer: MediaPlayer? = null

    override val layoutResId: Int
        get() = R.layout.activity_splash

    override val viewModelClass: KClass<SplashViewModel>
        get() = SplashViewModel::class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        window?.statusBarColor = Color.TRANSPARENT

        textureView.surfaceTextureListener = this

        viewModel.percentage
                .map {
                    "Percentage : %.2f".format(it * 100)
                }
                .onErrorReturn {
                    "Error: ${it.localizedMessage}"
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (BuildConfig.DEBUG) {
                        if (it.contains("Error")) {
                            percentText.text = it
                            percentText.visibility = View.VISIBLE
                        } else {
                            percentText.visibility = View.GONE
                        }
                    }
                }
                .disposedBy(disposeBag)

        viewModel.percentage
                .subscribe {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        progressBar.setProgress((it * 100).toInt(), true)
                    } else {
                        progressBar.progress = (it * 100).toInt()
                    }
                }.disposedBy(disposeBag)
    }

    override fun onStart() {
        super.onStart()
        viewModel.navigateTo
                .subscribe {
                    when (it) {
                        is Navigate.Forward -> {
                            when (it.endpoint) {
                                is SplashViewModel.NavigationEndpoint.Loading -> {
                                    splashChrome.post {
                                        splashChrome.animate().alpha(0f).start()
                                    }
                                    mMediaPlayer!!.start()
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

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
        return true
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
        val s = Surface(p0)

        try {
            val afd = assets.openFd("splash.mp4")
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length);
            mediaPlayer.setSurface(s)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnCompletionListener {
                var intent = NavigationConstants.HOME.asDeepLinkIntent()
                startActivity(intent)
                finish()
            }
            mMediaPlayer = mediaPlayer
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}