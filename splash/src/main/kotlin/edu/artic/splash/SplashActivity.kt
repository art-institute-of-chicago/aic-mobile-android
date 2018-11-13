package edu.artic.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.DialogFragment.STYLE_NO_FRAME
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.support.annotation.UiThread
import android.transition.Fade
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import com.fuzz.rx.disposedBy
import edu.artic.base.PermissibleError
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.makeStatusBarTransparent
import edu.artic.localization.ui.LanguageSettingsFragment
import edu.artic.navigation.NavigationConstants
import edu.artic.util.handleNetworkError
import edu.artic.viewmodel.BaseViewModelActivity
import edu.artic.viewmodel.Navigate
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_splash.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass


class SplashActivity : BaseViewModelActivity<SplashViewModel>(), TextureView.SurfaceTextureListener {
    private var mMediaPlayer: MediaPlayer? = null
    private lateinit var surface: Surface

    override val layoutResId: Int
        get() = R.layout.activity_splash

    override val viewModelClass: KClass<SplashViewModel>
        get() = SplashViewModel::class

    override val shouldRecreateUponLanguageChange = false

    private lateinit var fadeInAnimation: ViewPropertyAnimator
    private var errorDialog: AlertDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        with(window) {
            requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            exitTransition = Fade()
        }

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

                        Timber.e(it)
                    }

                    /**
                     * Display alert with error message.
                     */
                    errorDialog?.dismiss()
                    errorDialog = AlertDialog.Builder(this, R.style.ErrorDialog)
                            .setTitle(resources.getString(R.string.errorDialogTitle))
                            .setMessage(errorMessage)
                            .setOnDismissListener { _ ->
                                if (it is PermissibleError) {
                                    viewModel.proceedToWelcomePageIfDataAvailable()
                                }
                            }
                            .setPositiveButton(getString(android.R.string.ok)) { dialog, _->
                                dialog.dismiss()
                            }
                            .show()
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
                .filter { it is Navigate.Forward }
                .map { it as Navigate.Forward }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { navigation ->
                    val endpoint = navigation.endpoint
                    when (endpoint) {
                        is SplashViewModel.NavigationEndpoint.StartVideo ->
                            fadeOutChrome(endpoint.displayLanguageSettings)
                        is SplashViewModel.NavigationEndpoint.Welcome ->
                            goToWelcomeActivity()
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

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, width: Int, height: Int) {
        surface = Surface(p0)
        updateTextureViewSize(width, height)

        try {
            val afd = resources.openRawResourceFd(R.raw.splash)
            val mediaPlayer = MediaPlayer()
            mMediaPlayer = mediaPlayer
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mediaPlayer.setSurface(surface)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnCompletionListener {
                goToWelcomeActivity()
            }
        } catch (ignored: Throwable) {
            ///TODO: instead, handle errors when we receive the Navigate.Forward event (i.e. when the progressBar is full)
        }
    }

    private fun fadeOutChrome(displayDialog: Boolean) {
        textureView.alpha = 0f
        mMediaPlayer?.seekTo(0)
        splashChrome.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(p0: Animator?) {
                fadeInVideo(displayDialog)
            }
        }).start()
    }

    private fun fadeInVideo(displayDialog: Boolean) {
        animatedMuseum.animate().alpha(0f).start()
        textureView.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(p0: Animator?) {
                mMediaPlayer?.start()
                if (displayDialog) {
                    pauseVideo(4)
                }
            }
        }).start()
    }

    private fun goToWelcomeActivity() {
        val intent = NavigationConstants.HOME.asDeepLinkIntent()
        val options = ActivityOptions
                .makeSceneTransitionAnimation(this, textureView, "museumImage")
        if (!isFinishing) {
            startActivity(intent, options.toBundle())
            finishAfterTransition()
        }
    }

    /**
     * If the application language has not been set,
     * pause the splash video and display language settings dialog.
     */
    private fun pauseVideo(time: Long) {
        Observable.timer(time, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = {
                            Timber.d(it.toString())
                            mMediaPlayer?.pause()
                            displayLanguageSelectionDialog()
                        },
                        onError = {
                            it.printStackTrace()
                        }
                )
                .disposedBy(disposeBag)
    }

    /**
     * Display Application Language Settings Dialog.
     * Resume the splash video after language is selected.
     * (i.e. after [LanguageSettingsFragment] is dismissed).
     */
    private fun displayLanguageSelectionDialog() {
        val fragment = LanguageSettingsFragment.getLanguageSettingsDialogForSplash()
        fragment.attachTourStateListener(object : LanguageSettingsFragment.LanguageSelectionListener {
            override fun languageSelected() {
                resumeVideo()
            }
        })
        fragment.setStyle(STYLE_NO_FRAME, R.style.SplashTheme)
        fragment.show(supportFragmentManager, "language_settings")
    }

    @UiThread
    private fun resumeVideo() {
        try {
            splashChrome.postDelayed({ mMediaPlayer?.start() }, 400)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    /**
     * Scale the size of the [TextureView].
     */
    private fun updateTextureViewSize(width: Int, height: Int) {
        val matrix = Matrix()
        val ratioOfScreen = height.toFloat() / width.toFloat()
        val videoRatio = 16f / 9f
        val scaleRatio = videoRatio / ratioOfScreen
        matrix.setScale(1.0f, scaleRatio, 0f, 0f)
        textureView.setTransform(matrix)

        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        val videoHeight = (width.toFloat() * videoRatio).toInt()
        val percentageOfScreenAboveMuseumInVideo = (636f/1280f)
        val topScreenOffset = (videoHeight * percentageOfScreenAboveMuseumInVideo)
        val imagePadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)
        params.topMargin = (topScreenOffset - imagePadding).toInt()
        animatedMuseum.layoutParams = params
    }
}