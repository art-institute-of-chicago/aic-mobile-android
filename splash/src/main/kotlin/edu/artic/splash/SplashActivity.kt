package edu.artic.splash

import android.app.AlertDialog
import android.app.DialogFragment.STYLE_NO_FRAME
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import artic.edu.localization.ui.LanguageSettingsFragment
import com.fuzz.rx.disposedBy
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.makeStatusBarTransparent
import edu.artic.navigation.NavigationConstants
import edu.artic.util.handleNetworkError
import edu.artic.base.utils.setWindowFlag
import edu.artic.viewmodel.BaseViewModelActivity
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_splash.*
import kotlin.reflect.KClass


class SplashActivity : BaseViewModelActivity<SplashViewModel>(), TextureView.SurfaceTextureListener {
    private var mMediaPlayer: MediaPlayer? = null

    override val layoutResId: Int
        get() = R.layout.activity_splash

    override val viewModelClass: KClass<SplashViewModel>
        get() = SplashViewModel::class

    private lateinit var fadeInAnimation: ViewPropertyAnimator
    private var errorDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeStatusBarTransparent()

        textureView.surfaceTextureListener = this
        viewModel.percentage
                .handleNetworkError(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        progressBar.setProgress((it * 100).toInt(), true)
                    } else {
                        progressBar.progress = (it * 100).toInt()
                    }
                }, onError = {
                    /**
                     * Display error message below the progressbar.
                     */
                    val errorMessage = it.localizedMessage

                    if (BuildConfig.DEBUG) {
                        percentText.visibility = View.VISIBLE
                        percentText.text = errorMessage
                    }

                    /**
                     * Display alert with error message.
                     *
                     * TODO: localize the error strings.
                     */
                    errorDialog?.dismiss()
                    errorDialog = AlertDialog.Builder(this, R.style.ErrorDialog)
                            .setTitle(resources.getString(R.string.errorDialogTitle))
                            .setMessage(errorMessage)
                            .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                                dialog.dismiss()
                            }.show()

                }).disposedBy(disposeBag)


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
        errorDialog?.dismiss()
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
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mediaPlayer.setSurface(s)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnCompletionListener {
                handleAnimationCompletion()
            }
            updateTextureViewSize(p1, p2)
            mMediaPlayer = mediaPlayer
        } catch (ignored: Throwable) {
            ///TODO: instead, handle errors when we receive the Navigate.Forward event (i.e. when the progressBar is full)
        }
    }

    private fun handleAnimationCompletion() {
        val fragment = LanguageSettingsFragment()
        fragment.setStyle(STYLE_NO_FRAME, R.style.SplashTheme)
        fragment.show(supportFragmentManager,"language_settings")
//        var intent = NavigationConstants.HOME.asDeepLinkIntent()
//        startActivity(intent)
//        finish()
    }

    private fun updateTextureViewSize(width: Int, height: Int) {
        val matrix = Matrix()
        var ratioOfScreen = height.toFloat() / width.toFloat()
        var ratio = (16f / 9f) / ratioOfScreen
        matrix.setScale(1.0f, ratio, width / 2f, height / 2f)
        textureView.setTransform(matrix)
    }
}