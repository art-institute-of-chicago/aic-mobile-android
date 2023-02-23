package edu.artic.ui

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.fuzz.rx.DisposeBag
import com.google.android.material.appbar.CollapsingToolbarLayout
import dagger.android.support.AndroidSupportInjection
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.getThemeColors
import edu.artic.base.utils.setWindowFlag
import edu.artic.view.ArticMainAppBarLayout
import java.lang.reflect.ParameterizedType
import javax.inject.Inject


abstract class BaseFragment<VB : ViewBinding> : androidx.fragment.app.DialogFragment(),
    OnBackPressedListener {

    var toolbar: Toolbar? = null
    var collapsingToolbar: CollapsingToolbarLayout? = null

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Binding is not available yet.")

    @get:StringRes
    protected abstract val title: Int

    private var requestedTitle: String? = null

    /**
     * This method can be used to manually update
     *
     * * [requestedTitle] (which can be retrieved later with [getToolbarTitle])
     * * The [activity's title][BaseActivity.setTitle]
     * and
     * * (If present) the [collapsing toolbar][collapsingToolbar]'s title
     *
     * C.f. [updateToolbar]
     */
    fun requestTitleUpdate(proposedTitle: String) {

        baseActivity.runOnUiThread {
            this.requestedTitle = proposedTitle
            /**
             * This assignment to BaseActivity.title
             *
             * 1. updates the title in regular [Toolbar]s
             * 2. **does not** update the title in [CollapsingToolbarLayout]s
             */
            baseActivity.title = proposedTitle

            /**
             * Ensure [toolbar] and [collapsingToolbar] have had a chance to be bound
             */
            updateToolbar(requireView())
            collapsingToolbar?.run {
                title = proposedTitle
                parent.let {
                    if (it is ArticMainAppBarLayout) {
                        it.adaptExpandedTextAppearance()
                    }
                }
            }
        }
    }

    private fun getToolbarTitle(): String {
        return requestedTitle ?: getString(title)
    }

    abstract val screenName: ScreenName?

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    val baseActivity: BaseActivity<*>
        get() = activity as BaseActivity<*>

    /**
     * # Lifecycle: `init{}` -> [onDestroyView]
     *
     * Do not add navigation observers to this; those belong in [navigationDisposeBag].
     */
    val disposeBag = DisposeBag()

    /**
     * # Lifecycle: [onResume] -> [onPause]
     */
    val navigationDisposeBag = DisposeBag()

    protected val navController
        get() = Navigation.findNavController(requireView())

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        screenName?.let {
            analyticsTracker.reportScreenView(baseActivity, it)
        }
    }

    override fun onResume() {
        super.onResume()
        // We can wait to update the toolbar text...
        requireView().post {
            view?.let {
                updateToolbar(it)
            }
        }
        // ...but we need to set status bar color RIGHT NOW (c.f. 'updateWindowProperties' docs)
        updateWindowProperties()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val vbClass =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        val method = vbClass.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        _binding = method.invoke(null, inflater, container, false) as VB
        return binding.root
    }

    protected open fun hasTransparentStatusBar(): Boolean = false

    protected open val customToolbarColorResource: Int
        get() = 0

    /**
     * If it is set, the status bar is painted with statusBarColor.
     */
    protected open val overrideStatusBarColor: Boolean
        get() = true

    protected open fun hasHomeAsUpEnabled(): Boolean = true

    @UiThread
    private fun updateToolbar(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        if (toolbar != null) {
            baseActivity.setSupportActionBar(toolbar)
            baseActivity.supportActionBar?.apply {
                setDisplayShowTitleEnabled(true)
                setDisplayHomeAsUpEnabled(hasHomeAsUpEnabled())
                setDisplayShowHomeEnabled(true)
            }

            baseActivity.title = getString(title)
        }

        collapsingToolbar = view.findViewById(R.id.collapsingToolbar)
        collapsingToolbar?.parent?.let {
            if (it is ArticMainAppBarLayout) {
                it.adaptExpandedTextAppearance()
            }
        }

    }

    /**
     * # Usage
     *
     * Call this at any point after [onCreate] to change the appearance of the host's
     * [status bar][Window.setStatusBarColor].
     *
     * Does nothing if [overrideStatusBarColor] returns false.
     *
     * # Why is the fragment drawing partially _under_ the nav buttons?
     *
     * Simple answer: because you [posted][View.post] this method call instead of calling
     * inline in [BaseActivity.onResumeFragments] or [onResume].
     *
     * # But it's only doing that sometimes. What's wrong with posting the method call?
     *
     * If this is called on _just_ the right frame after [onResume] returns, our host's
     * DecorView might miscalculate the window inset. You can see this easily in the
     * SDK 26 emulator image by [posting a call][getView] to this method in [onResume]
     * and then rotating the device.
     *
     * I wasn't able to track down the precise cause, but it seems that it's strongly
     * related to the inset calculations in
     *
     * `com.android.internal.policy.DecorView`'s `updateColorViews` function.
     *
     *
     * The insets are set much earlier, when [BaseActivity.setContentView] actually
     * instantiates the `DecorView`. The call to `updateColorViews` triggered here
     * by [assigning statusBarColor][Window.setStatusBarColor] passes along a null
     * `WindowInsets` object, which means that cached inset values are used for layout
     * instead of new ones.
     *
     *
     * The only way I found to recompute the insets once you reach that state is to
     * call [view.getRootView()][View.getRootView] [.forceLayout()][View.forceLayout]
     * on the following frame. Of course, you can sidestep the whole issue by calling
     * this sufficiently early in the fragment lifecycle. In [onResume], for example.
     */
    @UiThread
    private fun updateWindowProperties() {
        if (overrideStatusBarColor) {
            // Since this is always run on UI thread, the activity and context won't
            // change for the duration of this call.
            val act = requireActivity()
            val ctx = requireContext()
            val decorView = act.window.decorView

            if (hasTransparentStatusBar()) {

                var uiOptions = decorView.systemUiVisibility
                uiOptions =
                    uiOptions or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

                act.setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
                act.window?.statusBarColor = Color.TRANSPARENT
                decorView.systemUiVisibility = uiOptions

            } else {
                /**
                 * Reset the decor to display the status bar.
                 */
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                act.setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)

                if (customToolbarColorResource == 0) {
                    val primaryDarkColor = intArrayOf(android.R.attr.colorPrimary)
                    ctx.getThemeColors(primaryDarkColor).getOrNull(0)?.defaultColor?.let {
                        act.window?.statusBarColor = it
                    }
                } else {
                    act.window?.statusBarColor =
                        ContextCompat.getColor(ctx, customToolbarColorResource)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        disposeBag.clear()
    }

    override fun onDestroy() {
        // fix for bug where viewmodel store is not cleared on 27.1.0, might be fixed later.
        val activity = activity
        if (activity != null
            && activity.isFinishing
            && !activity.isChangingConfigurations
        ) {
            viewModelStore.clear()
        }
        super.onDestroy()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

}

interface OnBackPressedListener {
    fun onBackPressed(): Boolean
}
