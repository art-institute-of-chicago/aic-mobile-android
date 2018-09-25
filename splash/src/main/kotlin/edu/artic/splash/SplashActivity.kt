package edu.artic.splash

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
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

    private lateinit var fadeInAnimation: ViewPropertyAnimator

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

        welcome.alpha = 0f

        fadeInAnimation = welcome.animate()
                .alpha(1f)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setDuration(1000)
                .setStartDelay(500)

        fadeInAnimation.start()
    }

    override fun onStart() {
        super.onStart()
        viewModel.navigateTo
                .subscribe {
                    when (it) {
                        is Navigate.Forward -> {
                            when (it.endpoint) {
                                is SplashViewModel.NavigationEndpoint.Loading -> {
                                    mMediaPlayer!!.start()
                                    splashChrome.postDelayed(Runnable {
                                        splashChrome.animate().alpha(0f).start()
                                    }, 200)
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
        fadeInAnimation.cancel()
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
                handleAnimationCompletion()
            }
            mMediaPlayer = mediaPlayer
            updateTextureViewSize(p1, p2)
        } catch (e: Throwable) {
            handleAnimationCompletion()
        }
    }

    private fun handleAnimationCompletion() {
        //TODO (Sameer) add language here and use Model
        var intent = NavigationConstants.HOME.asDeepLinkIntent()
        startActivity(intent)
        finish()
    }

    private fun updateTextureViewSize(width: Int, height: Int) {
        val matrix = Matrix()
        var ratioOfScreen = height.toFloat() / width.toFloat()
        var ratio = (16f / 9f) / ratioOfScreen
        matrix.setScale(1.0f, ratio, width / 2f, height / 2f)
        textureView.setTransform(matrix)
    }
}